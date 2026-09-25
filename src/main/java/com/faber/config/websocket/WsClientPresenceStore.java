package com.faber.config.websocket;

import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/** 跨节点共享的 WebSocket 在线连接索引；每条记录按心跳窗口自动过期。 */
@Component
public class WsClientPresenceStore {
    public static final long ONLINE_TTL_SECONDS = 60;
    private static final String REDIS_KEY = "ws:client-presence:connections";

    @Resource
    private RedissonClient redisson;
    @Value("${spring.data.redis.prefix}")
    private String prefix;

    private RMapCache<String, WsClientPresenceRecord> records() {
        return redisson.getMapCache(prefix + ":" + REDIS_KEY);
    }

    public void refresh(WsClientInfoEntity client) {
        if (client == null || client.getSession() == null) return;

        String sessionId = client.getSession().getId();
        WsClientPresenceRecord record = new WsClientPresenceRecord();
        record.setSessionId(sessionId);
        record.setUserId(client.getUser() == null ? null : client.getUser().getId());
        // 未升级的客户端使用连接 ID 作为兼容标识；它不会跨连接持久化。
        record.setClientInstanceId(StrUtil.blankToDefault(client.getClientInstanceId(), sessionId));
        record.setClientType(client.getClientType());
        record.setRuntime(client.getRuntime());
        record.setAppCode(client.getAppCode());
        record.setAppName(client.getAppName());
        record.setRelease(client.getRelease());
        record.setEnvironment(client.getEnvironment());
        record.setPlatform(client.getPlatform());
        record.setOsName(client.getOsName());
        record.setOsVersion(client.getOsVersion());
        record.setDeviceModel(client.getDeviceModel());
        record.setConnectedAt(client.getConnectedAt());
        record.setLastSeenAt(client.getLastSeenAt());

        records().fastPut(sessionId, record, ONLINE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    public void remove(String sessionId) {
        if (!StrUtil.isBlank(sessionId)) {
            records().fastRemove(sessionId);
        }
    }

    public WsClientPresenceRecord get(String sessionId) {
        return StrUtil.isBlank(sessionId) ? null : records().get(sessionId);
    }

    public Map<String, WsClientPresenceRecord> all() {
        return records().readAllMap();
    }
}
