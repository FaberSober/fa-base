package com.faber.config.websocket;

import com.faber.api.base.admin.entity.User;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WsClientPresenceStoreTest {

    @Test
    void legacyClientUsesConnectionIdAndKeepsThePresenceTtl() {
        @SuppressWarnings("unchecked")
        RMapCache<String, WsClientPresenceRecord> records = mock(RMapCache.class);
        RedissonClient redisson = mock(RedissonClient.class);
        when(redisson.<String, WsClientPresenceRecord>getMapCache("test:ws:client-presence:connections"))
                .thenReturn(records);
        WsClientPresenceStore store = new WsClientPresenceStore();
        ReflectionTestUtils.setField(store, "redisson", redisson);
        ReflectionTestUtils.setField(store, "prefix", "test");

        Session session = mock(Session.class);
        when(session.getId()).thenReturn("legacy-socket");
        User user = new User();
        user.setId("42");
        WsClientInfoEntity client = new WsClientInfoEntity();
        client.setSession(session);
        client.setUser(user);
        client.setClientType("MOBILE");
        client.setConnectedAt(1_000L);
        client.setLastSeenAt(2_000L);

        store.refresh(client);

        ArgumentCaptor<WsClientPresenceRecord> record = ArgumentCaptor.forClass(WsClientPresenceRecord.class);
        verify(records).fastPut(eq("legacy-socket"), record.capture(),
                eq(WsClientPresenceStore.ONLINE_TTL_SECONDS), eq(TimeUnit.SECONDS));
        assertEquals("legacy-socket", record.getValue().getClientInstanceId());
        assertEquals("42", record.getValue().getUserId());
        assertEquals(1_000L, record.getValue().getConnectedAt());
        assertEquals(2_000L, record.getValue().getLastSeenAt());
    }
}
