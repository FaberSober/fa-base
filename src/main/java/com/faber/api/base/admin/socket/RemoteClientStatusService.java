package com.faber.api.base.admin.socket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.faber.api.base.admin.biz.RemoteClientBiz;
import com.faber.config.websocket.WsBaseService;
import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.config.websocket.WsClientPresenceRecord;
import com.faber.config.websocket.WsClientPresenceStore;
import com.faber.core.annotation.FaWsService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** 当前页面的临时状态订阅；Redis 心跳索引和消息主题覆盖跨节点场景。 */
@Service
@Slf4j
@FaWsService(type = RemoteClientStatusService.TYPE)
public class RemoteClientStatusService implements WsBaseService {
    public static final String TYPE = "RemoteClientStatus";
    @Resource private RemoteClientBiz remoteClientBiz;
    @Resource private WsClientPresenceStore presenceStore;
    @Resource private RedissonClient redisson;
    @Value("${spring.data.redis.prefix}") private String prefix;
    private RTopic topic;
    private int listenerId;
    private final Map<String, Watch> watches = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        topic = redisson.getTopic(prefix + ":ws:remote-client-status", StringCodec.INSTANCE);
        listenerId = topic.addListener(String.class, (channel, value) -> {
            JSONObject target = new JSONObject(value);
            watches.values().stream()
                    .filter(watch -> Objects.equals(watch.userId, target.getStr("userId"))
                            && Objects.equals(watch.instanceId, target.getStr("clientInstanceId")))
                    .forEach(this::sendSnapshot);
        });
    }

    @PreDestroy
    public void destroy() {
        if (topic != null) topic.removeListener(listenerId);
        watches.clear();
    }

    public void publish(WsClientInfoEntity client) {
        if (client.getClientType() == null || client.getUser() == null || client.getSession() == null) return;
        String instanceId = StrUtil.blankToDefault(client.getClientInstanceId(), client.getConnectionId());
        topic.publish(new JSONObject(Map.of("userId", client.getUser().getId(), "clientInstanceId", instanceId)).toString());
    }

    @Override
    public void onMessage(WsClientInfoEntity admin, JSONObject message) {
        if (message == null) return;
        String watchId = message.getStr("watchId");
        if (StrUtil.isBlank(watchId) || watchId.length() > 128) return;
        String adminId = admin.getSession().getId();
        String key = adminId + ":" + watchId;
        if ("unwatch".equals(message.getStr("action"))) {
            watches.remove(key);
            return;
        }
        if (!"watch".equals(message.getStr("action"))) return;
        try {
            remoteClientBiz.requireAccess(admin.getUser() == null ? null : admin.getUser().getId(), admin.getToken());
            String userId = message.getStr("userId");
            String instanceId = message.getStr("clientInstanceId");
            if (StrUtil.isBlank(userId) || userId.length() > 128 || StrUtil.isBlank(instanceId) || instanceId.length() > 128) {
                admin.sendMessage(TYPE, Map.of("action", "error", "watchId", watchId, "state", "unknown",
                        "serverTime", System.currentTimeMillis(), "message", "客户端稳定标识无效，请重新打开页面"));
                return;
            }
            Watch watch = new Watch(watchId, admin, userId, instanceId);
            watches.put(key, watch);
            sendSnapshot(watch);
        } catch (RuntimeException e) {
            admin.sendMessage(TYPE, Map.of("action", "error", "watchId", watchId, "state", "unknown",
                    "serverTime", System.currentTimeMillis(), "message", "无法订阅客户端状态，请检查权限或服务连接"));
        }
    }

    @Override
    public void onClose(WsClientInfoEntity client) {
        if (client.getSession() != null) watches.entrySet().removeIf(entry -> entry.getValue().admin == client);
    }

    @Scheduled(fixedDelay = 5_000L)
    public void refreshWatches() {
        watches.values().forEach(this::sendSnapshot);
    }

    private void sendSnapshot(Watch watch) {
        if (!watch.admin.getSession().isOpen()) {
            watches.remove(watch.admin.getSession().getId() + ":" + watch.id, watch);
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Object> event = new HashMap<>();
        event.put("action", "status");
        event.put("watchId", watch.id);
        event.put("serverTime", now);
        try {
            remoteClientBiz.requireAccess(watch.admin.getUser().getId(), watch.admin.getToken());
        } catch (RuntimeException e) {
            watches.remove(watch.admin.getSession().getId() + ":" + watch.id, watch);
            event.put("action", "error");
            event.put("state", "unknown");
            event.put("message", "状态订阅权限已失效");
            watch.admin.sendMessage(TYPE, event);
            return;
        }
        try {
            WsClientPresenceRecord client = presenceStore.all().values().stream()
                    .filter(record -> watch.userId.equals(record.getUserId()) && watch.instanceId.equals(record.getClientInstanceId()))
                    .filter(record -> now - record.getLastSeenAt() <= 60_000L)
                    .max(Comparator.comparingLong(WsClientPresenceRecord::getConnectedAt)
                            .thenComparing(WsClientPresenceRecord::getSessionId)).orElse(null);
            event.put("state", client == null ? "offline" : now - client.getLastSeenAt() > 40_000L ? "delayed" : "online");
            if (client != null) {
                watch.lastSeenAt = client.getLastSeenAt();
                event.put("clientId", client.getSessionId());
                event.put("connectedAt", client.getConnectedAt());
            }
            if (watch.lastSeenAt > 0) event.put("lastSeenAt", watch.lastSeenAt);
        } catch (RuntimeException e) {
            event.put("state", "unknown");
            event.put("message", "在线状态暂时不可用");
            log.warn("远程客户端状态快照读取失败: {}", e.getMessage());
        }
        watch.admin.sendMessage(TYPE, event);
    }

    private static final class Watch {
        private final String id;
        private final WsClientInfoEntity admin;
        private final String userId;
        private final String instanceId;
        private volatile long lastSeenAt;

        private Watch(String id, WsClientInfoEntity admin, String userId, String instanceId) {
            this.id = id;
            this.admin = admin;
            this.userId = userId;
            this.instanceId = instanceId;
        }
    }
}
