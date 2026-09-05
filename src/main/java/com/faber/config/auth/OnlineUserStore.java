package com.faber.config.auth;

import jakarta.annotation.Resource;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

/** 独立的会话索引，带条目 TTL；不扫描 Redis keyspace 或 Sa-Token 的全部键。 */
@Component
public class OnlineUserStore {
    @Resource
    private RedissonClient redisson;
    @Value("${spring.data.redis.prefix}")
    private String prefix;

    private RMapCache<String, OnlineUserSession> sessions() {
        return redisson.getMapCache(prefix + ":online-user:sessions");
    }

    public OnlineUserSession get(String id) {
        return sessions().get(id);
    }

    public void put(OnlineUserSession session, long timeoutSeconds) {
        sessions().fastPut(session.getId(), session, Math.max(0, timeoutSeconds), TimeUnit.SECONDS);
    }

    public void remove(String id) {
        sessions().fastRemove(id);
    }

    public Collection<OnlineUserSession> all() {
        return sessions().readAllValues();
    }

    public long revision() {
        return redisson.getAtomicLong(prefix + ":online-user:revision").get();
    }

    public void invalidate() {
        redisson.getAtomicLong(prefix + ":online-user:revision").incrementAndGet();
    }
}
