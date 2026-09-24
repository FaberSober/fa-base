package com.faber.api.base.admin.socket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.faber.api.base.admin.biz.RemoteClientBiz;
import com.faber.config.websocket.WsBaseService;
import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.core.annotation.FaWsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service
@FaWsService(type = RemoteLogWebSocketService.TYPE)
@Slf4j
public class RemoteLogWebSocketService implements WsBaseService {
    public static final String TYPE = "RemoteLog";
    private static final String CONTROL_TYPE = "RemoteLogControl";
    private static final long SESSION_TTL_MILLIS = 30 * 60_000L;
    private static final int MAX_LOG_LENGTH = 4_000;
    private static final int MAX_LOGS_PER_SECOND = 30;
    private static final Set<String> LEVELS = Set.of("DEBUG", "LOG", "INFO", "WARN", "ERROR");
    private static final Pattern SENSITIVE_VALUE = Pattern.compile(
            "(?i)([\\\"']?(?:password|passwd|pwd|access_token|refresh_token|token|authorization|cookie|secret|credential|session|signature)[\\\"']?\\s*[:=]\\s*)(?:\\\"[^\\\"]*\\\"|'[^']*'|[^,;&}\\]]+)"
    );
    private static final Pattern BEARER = Pattern.compile("(?i)\\bBearer\\s+[A-Za-z0-9._~+/-]+=*");

    @Resource
    private RemoteClientBiz remoteClientBiz;

    private final Map<String, CaptureSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, CaptureSession> sessionsByAdmin = new ConcurrentHashMap<>();
    private final Map<String, CaptureSession> sessionsByClient = new ConcurrentHashMap<>();

    @Override
    public void onMessage(WsClientInfoEntity entity, JSONObject message) {
        if (message == null) return;
        String action = message.getStr("action");
        if ("start".equals(action)) {
            if (!hasPermission(entity)) {
                sendError(entity, message.getStr("clientId"), "无远程日志权限");
                return;
            }
            startCapture(entity, message.getStr("clientId"));
        } else if ("stop".equals(action)) {
            if (!hasPermission(entity)) {
                sendError(entity, null, "无远程日志权限");
                return;
            }
            stopCapture(entity, message.getStr("sessionId"));
        } else if ("entry".equals(action)) {
            relayLog(entity, message);
        }
    }

    @Override
    public void onClose(WsClientInfoEntity entity) {
        String connectionId = connectionId(entity);
        for (CaptureSession capture : List.copyOf(sessions.values())) {
            if (connectionId.equals(connectionId(capture.admin)) || connectionId.equals(connectionId(capture.client))) {
                finish(capture, "ended", "connection_closed", capture.admin != entity);
            }
        }
    }

    @Scheduled(fixedDelay = 30_000L)
    public void expireSessions() {
        long now = System.currentTimeMillis();
        for (CaptureSession capture : List.copyOf(sessions.values())) {
            if (capture.expiresAt <= now) finish(capture, "ended", "timeout", true);
        }
    }

    private boolean hasPermission(WsClientInfoEntity admin) {
        if (admin.getUser() == null) return false;
        try {
            remoteClientBiz.requireAccess(admin.getUser().getId(), admin.getToken());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private synchronized void startCapture(WsClientInfoEntity admin, String clientId) {
        if (StrUtil.isBlank(clientId) || clientId.length() > 128) {
            sendError(admin, clientId, "客户端标识无效");
            return;
        }
        WsClientInfoEntity client = remoteClientBiz.getOnlineClient(clientId);
        if (client == null) {
            sendError(admin, clientId, "客户端已离线");
            return;
        }

        String adminId = connectionId(admin);
        String clientConnectionId = connectionId(client);
        CaptureSession existingByAdmin = sessionsByAdmin.get(adminId);
        if (existingByAdmin != null && existingByAdmin.client == client) {
            sendStarted(admin, existingByAdmin);
            return;
        }
        CaptureSession existingByClient = sessionsByClient.get(clientConnectionId);
        if (existingByClient != null) {
            if (existingByClient.admin == admin) {
                sendStarted(admin, existingByClient);
                return;
            }
            sendError(admin, clientId, "该客户端正在被其他管理员采集");
            return;
        }
        if (existingByAdmin != null) finish(existingByAdmin, "ended", "replaced", true);

        long now = System.currentTimeMillis();
        CaptureSession capture = new CaptureSession(UUID.randomUUID().toString(), admin, client, now + SESSION_TTL_MILLIS);
        sessions.put(capture.id, capture);
        sessionsByAdmin.put(adminId, capture);
        sessionsByClient.put(clientConnectionId, capture);
        client.sendMessage(CONTROL_TYPE, Map.of("action", "start", "sessionId", capture.id));
        sendStarted(admin, capture);
        log.info("远程日志采集开始: adminUserId={} clientUserId={}", userId(admin), userId(client));
    }

    private synchronized void stopCapture(WsClientInfoEntity admin, String requestedSessionId) {
        CaptureSession capture = sessionsByAdmin.get(connectionId(admin));
        if (capture == null) {
            admin.sendMessage(TYPE, Map.of("action", "stopped"));
            return;
        }
        if (StrUtil.isNotBlank(requestedSessionId) && !capture.id.equals(requestedSessionId)) {
            sendError(admin, null, "日志会话已结束");
            return;
        }
        finish(capture, "stopped", null, true);
    }

    private void relayLog(WsClientInfoEntity client, JSONObject message) {
        String sessionId = message.getStr("sessionId");
        if (StrUtil.isBlank(sessionId) || sessionId.length() > 40) return;
        CaptureSession capture = sessions.get(sessionId);
        if (capture == null || capture.client != client) return;
        long now = System.currentTimeMillis();
        if (capture.expiresAt <= now || !capture.admin.getSession().isOpen()) {
            finish(capture, "ended", capture.expiresAt <= now ? "timeout" : "admin_disconnected", false);
            return;
        }
        String level = message.getStr("level", "").toUpperCase(Locale.ROOT);
        if (!LEVELS.contains(level) || !capture.allowLog(now)) return;
        String text = sanitize(message.getStr("message"));
        if (text == null) return;
        String source = "runtime".equals(message.getStr("source")) ? "runtime" : "console";
        capture.admin.sendMessage(TYPE, Map.of(
                "action", "entry",
                "clientId", connectionId(client),
                "sessionId", capture.id,
                "level", level,
                "source", source,
                "message", text,
                "timestamp", now
        ));
    }

    private synchronized void finish(CaptureSession capture, String action, String reason, boolean notifyAdmin) {
        if (!sessions.remove(capture.id, capture)) return;
        sessionsByAdmin.remove(connectionId(capture.admin), capture);
        sessionsByClient.remove(connectionId(capture.client), capture);
        log.info("远程日志采集结束: adminUserId={} clientUserId={} action={} reason={}",
                userId(capture.admin), userId(capture.client), action, reason == null ? "manual" : reason);
        if (capture.client.getSession().isOpen()) {
            capture.client.sendMessage(CONTROL_TYPE, Map.of("action", "stop", "sessionId", capture.id));
        }
        if (notifyAdmin && capture.admin.getSession().isOpen()) {
            if (reason == null) {
                capture.admin.sendMessage(TYPE, Map.of("action", action, "sessionId", capture.id));
            } else {
                capture.admin.sendMessage(TYPE, Map.of(
                        "action", action,
                        "clientId", connectionId(capture.client),
                        "sessionId", capture.id,
                        "reason", reason
                ));
            }
        }
    }

    private void sendStarted(WsClientInfoEntity admin, CaptureSession capture) {
        admin.sendMessage(TYPE, Map.of(
                "action", "started",
                "clientId", connectionId(capture.client),
                "sessionId", capture.id,
                "expiresAt", capture.expiresAt
        ));
    }

    private void sendError(WsClientInfoEntity admin, String clientId, String message) {
        if (admin.getSession() == null || !admin.getSession().isOpen()) return;
        admin.sendMessage(TYPE, Map.of(
                "action", "error",
                "clientId", clientId == null ? "" : clientId,
                "message", message
        ));
    }

    private String sanitize(String value) {
        if (value == null) return null;
        String sanitized = BEARER.matcher(value).replaceAll("Bearer [REDACTED]");
        sanitized = SENSITIVE_VALUE.matcher(sanitized).replaceAll("$1\"[REDACTED]\"");
        String truncation = "…[truncated]";
        return sanitized.length() <= MAX_LOG_LENGTH
                ? sanitized
                : sanitized.substring(0, MAX_LOG_LENGTH - truncation.length()) + truncation;
    }

    private String connectionId(WsClientInfoEntity client) {
        return client.getSession() == null ? "" : client.getSession().getId();
    }

    private String userId(WsClientInfoEntity client) {
        return client.getUser() == null ? "" : client.getUser().getId();
    }

    private static final class CaptureSession {
        private final String id;
        private final WsClientInfoEntity admin;
        private final WsClientInfoEntity client;
        private final long expiresAt;
        private long rateWindowStartedAt;
        private int rateWindowCount;

        private CaptureSession(String id, WsClientInfoEntity admin, WsClientInfoEntity client, long expiresAt) {
            this.id = id;
            this.admin = admin;
            this.client = client;
            this.expiresAt = expiresAt;
        }

        private synchronized boolean allowLog(long now) {
            if (now - rateWindowStartedAt >= 1_000L) {
                rateWindowStartedAt = now;
                rateWindowCount = 0;
            }
            return ++rateWindowCount <= MAX_LOGS_PER_SECOND;
        }
    }
}
