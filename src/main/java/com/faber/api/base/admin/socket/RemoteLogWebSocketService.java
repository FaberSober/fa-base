package com.faber.api.base.admin.socket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.faber.api.base.admin.biz.RemoteClientBiz;
import com.faber.config.websocket.WsBaseService;
import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.config.websocket.WsChatEndpoint;
import com.faber.config.websocket.WsClientPresenceRecord;
import com.faber.config.websocket.WsClientPresenceStore;
import com.faber.core.annotation.FaWsService;
import jakarta.annotation.Resource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    @Resource private WsClientPresenceStore presenceStore;
    @Resource private RedissonClient redisson;
    @Value("${spring.data.redis.prefix}") private String prefix;
    private RTopic topic;
    private int listenerId;
    private RMapCache<String, String> clientOwners;

    private final Map<String, CaptureSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, CaptureSession> sessionsByAdmin = new ConcurrentHashMap<>();
    private final Map<String, CaptureSession> sessionsByClient = new ConcurrentHashMap<>();
    private final Map<String, LogRateWindow> incomingRates = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        clientOwners = redisson.getMapCache(prefix + ":ws:remote-log:owners", StringCodec.INSTANCE);
        topic = redisson.getTopic(prefix + ":ws:remote-log:route", StringCodec.INSTANCE);
        listenerId = topic.addListener(String.class, (channel, value) -> {
            try {
                onRoutedMessage(new JSONObject(value));
            } catch (RuntimeException e) {
                log.warn("远程日志路由处理失败: {}", e.getMessage());
            }
        });
    }

    @PreDestroy
    public void destroy() {
        for (CaptureSession capture : List.copyOf(sessions.values())) finish(capture, "ended", "admin_disconnected", false);
        if (topic != null) topic.removeListener(listenerId);
    }

    @Override
    public void onMessage(WsClientInfoEntity entity, JSONObject message) {
        if (message == null) return;
        String action = message.getStr("action");
        String requestId = message.getStr("requestId", "");
        if (requestId.length() > 128) return;
        if ("start".equals(action)) {
            if (!hasPermission(entity)) {
                sendError(entity, message.getStr("clientId"), requestId, "无远程日志权限");
                return;
            }
            if (StrUtil.isBlank(requestId)) {
                sendError(entity, message.getStr("clientId"), requestId, "请刷新管理端后重新开启采集");
                return;
            }
            try {
                startCapture(entity, message.getStr("clientId"), requestId, message.getLong("expiresAt"));
            } catch (RuntimeException e) {
                sendError(entity, message.getStr("clientId"), requestId, "采集服务暂时不可用，请稍后重试");
                log.warn("远程日志开启失败: {}", e.getMessage());
            }
        } else if ("stop".equals(action)) {
            if (!hasPermission(entity)) {
                sendError(entity, null, requestId, "无远程日志权限");
                return;
            }
            stopCapture(entity, message.getStr("sessionId"), requestId);
        } else if ("entry".equals(action) || "ack".equals(action)) {
            routeClientEvent(entity, message);
        }
    }

    @Override
    public void onClose(WsClientInfoEntity entity) {
        String connectionId = connectionId(entity);
        incomingRates.remove(connectionId);
        CaptureSession own = sessionsByAdmin.get(connectionId);
        if (own != null) finish(own, "ended", "admin_disconnected", false);
        topic.publish(new JSONObject(Map.of("kind", "closed", "clientId", connectionId)).toString());
    }

    @Scheduled(fixedDelay = 5_000L)
    public synchronized void expireSessions() {
        long now = System.currentTimeMillis();
        for (CaptureSession capture : List.copyOf(sessions.values())) {
            try {
                if (capture.expiresAt <= now) finish(capture, "ended", "timeout", true);
                else if (!capture.admin.getSession().isOpen()) finish(capture, "ended", "admin_disconnected", false);
                else if (!hasPermission(capture.admin)) finish(capture, "ended", "permission_lost", true);
                else {
                    WsClientPresenceRecord client = presenceStore.get(capture.client.getSessionId());
                    if (client == null || now - client.getLastSeenAt() > 60_000L) finish(capture, "ended", "connection_closed", true);
                    else if (!capture.acknowledged && now - capture.startedAt >= 15_000L) finish(capture, "ended", "ack_timeout", true);
                    else if (!claimClient(capture, true)) finish(capture, "ended", "replaced", true);
                }
            } catch (RuntimeException e) {
                log.warn("远程日志会话检查失败: {}", e.getMessage());
                finish(capture, "ended", "service_unavailable", true);
            }
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

    private synchronized void startCapture(WsClientInfoEntity admin, String clientId, String requestId, Long requestedExpiry) {
        if (StrUtil.isBlank(clientId) || clientId.length() > 128) {
            sendError(admin, clientId, requestId, "客户端标识无效");
            return;
        }
        long now = System.currentTimeMillis();
        WsClientPresenceRecord client = presenceStore.get(clientId);
        if (client == null || now - client.getLastSeenAt() > 60_000L) {
            sendError(admin, clientId, requestId, "客户端已离线");
            return;
        }

        String adminId = connectionId(admin);
        String clientConnectionId = client.getSessionId();
        CaptureSession existingByAdmin = sessionsByAdmin.get(adminId);
        if (existingByAdmin != null && existingByAdmin.requestId.equals(requestId)
                && existingByAdmin.client.getSessionId().equals(clientConnectionId)) {
            sendCaptureState(existingByAdmin);
            return;
        }
        if (existingByAdmin != null) finish(existingByAdmin, "ended", "replaced", true);
        long expiresAt = requestedExpiry == null ? now + SESSION_TTL_MILLIS : Math.min(requestedExpiry, now + SESSION_TTL_MILLIS);
        if (expiresAt <= now) {
            sendError(admin, clientId, requestId, "采集已超时，请手动重新开启");
            return;
        }
        CaptureSession capture = new CaptureSession(UUID.randomUUID().toString(), requestId, admin, client, now, expiresAt);
        if (!claimClient(capture, false)) {
            sendError(admin, clientId, requestId, "该客户端正在被其他管理员采集");
            return;
        }
        sessions.put(capture.id, capture);
        sessionsByAdmin.put(adminId, capture);
        sessionsByClient.put(clientConnectionId, capture);
        sendCaptureState(capture);
        sendControl(capture, "start");
        log.info("远程日志采集申请: adminUserId={} clientUserId={}", userId(admin), client.getUserId());
    }

    private synchronized void stopCapture(WsClientInfoEntity admin, String requestedSessionId, String requestId) {
        CaptureSession capture = sessionsByAdmin.get(connectionId(admin));
        if (capture == null) {
            admin.sendMessage(TYPE, Map.of("action", "stopped", "requestId", requestId));
            return;
        }
        if ((StrUtil.isNotBlank(requestedSessionId) && !capture.id.equals(requestedSessionId))
                || (!capture.requestId.equals(requestId))) {
            return;
        }
        finish(capture, "stopped", null, true);
    }

    private void routeClientEvent(WsClientInfoEntity client, JSONObject message) {
        String sessionId = message.getStr("sessionId");
        if (StrUtil.isBlank(sessionId) || sessionId.length() > 40) return;
        if (!sessionId.equals(clientOwners.get(connectionId(client)))) {
            client.sendMessage(CONTROL_TYPE, Map.of("action", "stop", "sessionId", sessionId));
            return;
        }
        JSONObject routed = new JSONObject();
        routed.set("kind", "event");
        routed.set("action", message.getStr("action"));
        routed.set("sessionId", sessionId);
        routed.set("clientId", connectionId(client));
        if ("entry".equals(message.getStr("action"))) {
            if (!incomingRates.computeIfAbsent(connectionId(client), ignored -> new LogRateWindow()).allow(System.currentTimeMillis())) return;
            String text = sanitize(message.getStr("message"));
            String level = message.getStr("level", "").toUpperCase(Locale.ROOT);
            if (text == null || !LEVELS.contains(level)) return;
            routed.set("level", level);
            routed.set("source", "runtime".equals(message.getStr("source")) ? "runtime" : "console");
            routed.set("message", text);
        }
        topic.publish(routed.toString());
    }

    private synchronized void onRoutedMessage(JSONObject message) {
        String kind = message.getStr("kind");
        String clientId = message.getStr("clientId");
        if ("control".equals(kind)) {
            JSONObject data = message.getJSONObject("data");
            if ("start".equals(data.getStr("action")) && !data.getStr("sessionId").equals(clientOwners.get(clientId))) return;
            WsClientInfoEntity client = WsChatEndpoint.getRemoteClientConnections().stream()
                    .filter(item -> clientId.equals(connectionId(item))).findFirst().orElse(null);
            if (client != null) client.sendMessage(CONTROL_TYPE, data);
            return;
        }
        if ("closed".equals(kind)) {
            CaptureSession capture = sessionsByClient.get(clientId);
            if (capture != null) finish(capture, "ended", "connection_closed", true);
            return;
        }
        if (!"event".equals(kind)) return;
        String sessionId = message.getStr("sessionId");
        CaptureSession capture = sessions.get(sessionId);
        if (capture == null || !capture.client.getSessionId().equals(clientId)) return;
        long now = System.currentTimeMillis();
        if (capture.expiresAt <= now || !capture.admin.getSession().isOpen()) {
            finish(capture, "ended", capture.expiresAt <= now ? "timeout" : "admin_disconnected", false);
            return;
        }
        if (!capture.acknowledged && now - capture.startedAt >= 15_000L) {
            finish(capture, "ended", "ack_timeout", true);
            return;
        }
        if ("ack".equals(message.getStr("action"))) {
            capture.acknowledged = true;
            sendCaptureState(capture);
            return;
        }
        if (!capture.acknowledged) return;
        String level = message.getStr("level", "").toUpperCase(Locale.ROOT);
        if (!LEVELS.contains(level) || !capture.allowLog(now)) return;
        String text = sanitize(message.getStr("message"));
        if (text == null) return;
        String source = "runtime".equals(message.getStr("source")) ? "runtime" : "console";
        capture.admin.sendMessage(TYPE, Map.of(
                "action", "entry",
                "clientId", clientId,
                "sessionId", capture.id,
                "requestId", capture.requestId,
                "level", level,
                "source", source,
                "message", text,
                "timestamp", now
        ));
    }

    private synchronized void finish(CaptureSession capture, String action, String reason, boolean notifyAdmin) {
        if (!sessions.remove(capture.id, capture)) return;
        sessionsByAdmin.remove(connectionId(capture.admin), capture);
        sessionsByClient.remove(capture.client.getSessionId(), capture);
        log.info("远程日志采集结束: adminUserId={} clientUserId={} action={} reason={}",
                userId(capture.admin), capture.client.getUserId(), action, reason == null ? "manual" : reason);
        try {
            clientOwners.remove(capture.client.getSessionId(), capture.id);
            sendControl(capture, "stop");
        } catch (RuntimeException e) {
            log.warn("远程日志会话占用释放失败: {}", e.getMessage());
        }
        if (notifyAdmin && capture.admin.getSession().isOpen()) {
            if (reason == null) {
                capture.admin.sendMessage(TYPE, Map.of("action", action, "sessionId", capture.id, "requestId", capture.requestId));
            } else {
                capture.admin.sendMessage(TYPE, Map.of(
                        "action", action,
                        "clientId", capture.client.getSessionId(),
                        "sessionId", capture.id,
                        "requestId", capture.requestId,
                        "reason", reason
                ));
            }
        }
    }

    private void sendCaptureState(CaptureSession capture) {
        capture.admin.sendMessage(TYPE, Map.of(
                "action", capture.acknowledged ? "started" : "starting",
                "clientId", capture.client.getSessionId(),
                "sessionId", capture.id,
                "requestId", capture.requestId,
                "expiresAt", capture.expiresAt,
                "serverTime", System.currentTimeMillis()
        ));
    }

    private void sendControl(CaptureSession capture, String action) {
        topic.publish(new JSONObject(Map.of("kind", "control", "clientId", capture.client.getSessionId(),
                "data", Map.of("action", action, "sessionId", capture.id))).toString());
    }

    private boolean claimClient(CaptureSession capture, boolean renew) {
        String clientId = capture.client.getSessionId();
        RLock lock = redisson.getLock(prefix + ":ws:remote-log:lock:" + clientId);
        lock.lock(10, TimeUnit.SECONDS);
        try {
            String owner = clientOwners.get(clientId);
            if (renew ? !capture.id.equals(owner) : owner != null) return false;
            clientOwners.fastPut(clientId, capture.id, 35, TimeUnit.SECONDS);
            return true;
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    private void sendError(WsClientInfoEntity admin, String clientId, String requestId, String message) {
        if (admin.getSession() == null || !admin.getSession().isOpen()) return;
        admin.sendMessage(TYPE, Map.of(
                "action", "error",
                "clientId", clientId == null ? "" : clientId,
                "requestId", requestId,
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
        return client.getConnectionId() == null ? "" : client.getConnectionId();
    }

    private String userId(WsClientInfoEntity client) {
        return client.getUser() == null ? "" : client.getUser().getId();
    }

    private static final class CaptureSession {
        private final String id;
        private final String requestId;
        private final WsClientInfoEntity admin;
        private final WsClientPresenceRecord client;
        private final long startedAt;
        private final long expiresAt;
        private volatile boolean acknowledged;
        private final LogRateWindow rateWindow = new LogRateWindow();

        private CaptureSession(String id, String requestId, WsClientInfoEntity admin, WsClientPresenceRecord client, long startedAt, long expiresAt) {
            this.id = id;
            this.requestId = requestId;
            this.admin = admin;
            this.client = client;
            this.startedAt = startedAt;
            this.expiresAt = expiresAt;
        }

        private synchronized boolean allowLog(long now) {
            return rateWindow.allow(now);
        }
    }

    private static final class LogRateWindow {
        private long rateWindowStartedAt;
        private int rateWindowCount;

        private synchronized boolean allow(long now) {
            if (now - rateWindowStartedAt >= 1_000L) {
                rateWindowStartedAt = now;
                rateWindowCount = 0;
            }
            return ++rateWindowCount <= MAX_LOGS_PER_SECOND;
        }
    }
}
