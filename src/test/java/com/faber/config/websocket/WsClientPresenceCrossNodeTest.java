package com.faber.config.websocket;

import com.faber.api.base.admin.entity.User;
import com.faber.core.config.redis.RedisProperties;
import com.faber.core.config.redis.RedissonSpringDataConfig;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = WsClientPresenceCrossNodeTest.RedisTestConfiguration.class)
class WsClientPresenceCrossNodeTest {

    @SpringBootConfiguration
    @EnableConfigurationProperties(RedisProperties.class)
    @Import(RedissonSpringDataConfig.class)
    static class RedisTestConfiguration {
    }

    @Autowired
    private RedissonClient firstNode;

    @Test
    void recordsAreSharedAcrossIndependentRedisClients() throws IOException {
        RedissonClient secondNode = Redisson.create(
                org.redisson.config.Config.fromJSON(firstNode.getConfig().toJSON()));
        String prefix = "adr013-cross-node-" + UUID.randomUUID();
        String sessionId = "socket-" + UUID.randomUUID();
        WsClientPresenceStore firstStore = store(firstNode, prefix);
        try {
            WsClientPresenceStore secondStore = store(secondNode, prefix);
            WsClientInfoEntity client = client(sessionId);

            firstStore.refresh(client);

            WsClientPresenceRecord shared = secondStore.get(sessionId);
            assertNotNull(shared);
            assertEquals("42", shared.getUserId());
            assertEquals("mobile-install-42", shared.getClientInstanceId());

            secondStore.remove(sessionId);
            assertNull(firstStore.get(sessionId));
        } finally {
            firstStore.remove(sessionId);
            secondNode.shutdown();
        }
    }

    private WsClientPresenceStore store(RedissonClient redisson, String prefix) {
        WsClientPresenceStore store = new WsClientPresenceStore();
        ReflectionTestUtils.setField(store, "redisson", redisson);
        ReflectionTestUtils.setField(store, "prefix", prefix);
        return store;
    }

    private WsClientInfoEntity client(String sessionId) {
        Session session = mock(Session.class);
        when(session.getId()).thenReturn(sessionId);
        User user = new User();
        user.setId("42");
        WsClientInfoEntity client = new WsClientInfoEntity();
        client.setSession(session);
        client.setUser(user);
        client.setClientType("MOBILE");
        client.setClientInstanceId("mobile-install-42");
        client.setConnectedAt(System.currentTimeMillis() - 1_000);
        client.setLastSeenAt(System.currentTimeMillis());
        return client;
    }
}
