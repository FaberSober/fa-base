package com.faber.api.base.admin.socket;

import cn.hutool.json.JSONObject;
import com.faber.api.base.admin.biz.RemoteClientBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.config.websocket.WsChatEndpoint;
import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.config.websocket.WsClientPresenceRecord;
import com.faber.config.websocket.WsClientPresenceStore;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemoteLogWebSocketServiceTest {
    @Test
    void losingTheClientBeforeAckIsRecoverableRatherThanAnAckFailure() {
        Fixture f = new Fixture();
        RemoteLogWebSocketService node = f.node();
        try (MockedStatic<WsChatEndpoint> endpoint = mockStatic(WsChatEndpoint.class)) {
            endpoint.when(WsChatEndpoint::getRemoteClientConnections).thenReturn(List.of(f.app));
            f.start(node, f.admin, "disconnect-before-ack", null);
            Map<?, ?> sessions = (Map<?, ?>) ReflectionTestUtils.getField(node, "sessions");
            ReflectionTestUtils.setField(sessions.values().iterator().next(), "startedAt", System.currentTimeMillis() - 20_000);
            when(f.presence.get("app-1")).thenReturn(null);
            node.expireSessions();
            assertEquals("connection_closed", f.lastEvent(f.admin).getStr("reason"));
        }
    }

    @Test
    void limitsTheLogRateBeforeRedisFanOut() {
        Fixture f = new Fixture();
        RemoteLogWebSocketService adminNode = f.node();
        RemoteLogWebSocketService appNode = f.node();
        try (MockedStatic<WsChatEndpoint> endpoint = mockStatic(WsChatEndpoint.class)) {
            endpoint.when(WsChatEndpoint::getRemoteClientConnections).thenReturn(List.of(f.app));
            f.start(adminNode, f.admin, "rate", null);
            String captureId = f.lastEvent(f.admin).getStr("sessionId");
            appNode.onMessage(f.app, f.event("ack", captureId));
            clearInvocations(f.topic);
            for (int i = 0; i < 35; i++) appNode.onMessage(f.app, f.event("entry", captureId).set("level", "INFO").set("message", "log"));
            verify(f.topic, times(30)).publish(anyString());
            assertEquals(30, f.messages.get(f.admin).stream().filter(event -> "entry".equals(event.getStr("action"))).count());
        }
    }

    @Test
    void captureWaitsForAckAndRoutesAcrossNodes() {
        Fixture f = new Fixture();
        RemoteLogWebSocketService adminNode = f.node();
        RemoteLogWebSocketService appNode = f.node();
        try (MockedStatic<WsChatEndpoint> endpoint = mockStatic(WsChatEndpoint.class)) {
            endpoint.when(WsChatEndpoint::getRemoteClientConnections).thenReturn(List.of(f.app));
            f.start(adminNode, f.admin, "request-1", null);
            JSONObject starting = f.lastEvent(f.admin);
            assertEquals("starting", starting.getStr("action"));
            String captureId = starting.getStr("sessionId");
            appNode.onMessage(f.app, f.event("entry", captureId).set("level", "INFO").set("message", "before ack"));
            assertEquals("starting", f.lastEvent(f.admin).getStr("action"));
            appNode.onMessage(f.app, f.event("ack", captureId));
            assertEquals("started", f.lastEvent(f.admin).getStr("action"));
            appNode.onMessage(f.app, f.event("entry", captureId).set("level", "INFO").set("message", "token=private-value"));
            JSONObject entry = f.lastEvent(f.admin);
            assertEquals("entry", entry.getStr("action"));
            assertEquals("request-1", entry.getStr("requestId"));
            assertFalse(entry.getStr("message").contains("private-value"));
            verify(f.app, atLeastOnce()).sendMessage(eq("RemoteLogControl"), any());
        }
    }

    @Test
    void stoppedSessionAndWrongConnectionCannotConfirmNewCapture() {
        Fixture f = new Fixture();
        RemoteLogWebSocketService node = f.node();
        try (MockedStatic<WsChatEndpoint> endpoint = mockStatic(WsChatEndpoint.class)) {
            endpoint.when(WsChatEndpoint::getRemoteClientConnections).thenReturn(List.of(f.app));
            f.start(node, f.admin, "old-request", null);
            String oldId = f.lastEvent(f.admin).getStr("sessionId");
            node.onMessage(f.admin, new JSONObject(Map.of("action", "stop", "requestId", "old-request")));
            f.start(node, f.admin, "new-request", null);
            JSONObject current = f.lastEvent(f.admin);
            String newId = current.getStr("sessionId");
            node.onMessage(f.app, f.event("ack", oldId));
            node.onMessage(f.client("wrong-connection", "app-user"), f.event("ack", newId));
            node.onMessage(f.admin, new JSONObject(Map.of("action", "stop", "requestId", "old-request", "sessionId", oldId)));
            assertEquals("starting", f.lastEvent(f.admin).getStr("action"));
            node.onMessage(f.app, f.event("ack", newId));
            assertEquals("new-request", f.lastEvent(f.admin).getStr("requestId"));
            assertEquals("started", f.lastEvent(f.admin).getStr("action"));
        }
    }

    @Test
    void onlyOneAdministratorCanOwnAClientAcrossNodes() {
        Fixture f = new Fixture();
        RemoteLogWebSocketService first = f.node();
        RemoteLogWebSocketService second = f.node();
        WsClientInfoEntity anotherAdmin = f.client("admin-2", "admin-user");
        try (MockedStatic<WsChatEndpoint> endpoint = mockStatic(WsChatEndpoint.class)) {
            endpoint.when(WsChatEndpoint::getRemoteClientConnections).thenReturn(List.of(f.app));
            f.start(first, f.admin, "first", null);
            f.start(second, anotherAdmin, "second", null);
            assertEquals("error", f.lastEvent(anotherAdmin).getStr("action"));
            assertTrue(f.lastEvent(anotherAdmin).getStr("message").contains("其他管理员"));
            first.onClose(f.admin);
            f.start(second, anotherAdmin, "third", null);
            assertEquals("starting", f.lastEvent(anotherAdmin).getStr("action"));
        }
    }

    @Test
    void clientDisconnectEndsCaptureAndRecoveryKeepsTheOriginalDeadline() {
        Fixture f = new Fixture();
        RemoteLogWebSocketService first = f.node();
        RemoteLogWebSocketService second = f.node();
        long deadline = System.currentTimeMillis() + 60_000;
        try (MockedStatic<WsChatEndpoint> endpoint = mockStatic(WsChatEndpoint.class)) {
            endpoint.when(WsChatEndpoint::getRemoteClientConnections).thenReturn(List.of(f.app));
            f.start(first, f.admin, "first", deadline);
            String oldCapture = f.lastEvent(f.admin).getStr("sessionId");
            second.onMessage(f.app, f.event("ack", oldCapture));
            second.onClose(f.app);
            assertEquals("connection_closed", f.lastEvent(f.admin).getStr("reason"));
            f.start(first, f.admin, "recovery", deadline);
            assertEquals(deadline, f.lastEvent(f.admin).getLong("expiresAt"));
            assertNotEquals(oldCapture, f.lastEvent(f.admin).getStr("sessionId"));
        }
    }

    @Test
    void missingAckAndExpiredPermissionTerminateCapture() {
        Fixture f = new Fixture();
        RemoteLogWebSocketService node = f.node();
        try (MockedStatic<WsChatEndpoint> endpoint = mockStatic(WsChatEndpoint.class)) {
            endpoint.when(WsChatEndpoint::getRemoteClientConnections).thenReturn(List.of(f.app));
            f.start(node, f.admin, "no-ack", null);
            Map<?, ?> sessions = (Map<?, ?>) ReflectionTestUtils.getField(node, "sessions");
            Object capture = sessions.values().iterator().next();
            ReflectionTestUtils.setField(capture, "startedAt", System.currentTimeMillis() - 20_000);
            node.expireSessions();
            assertEquals("ack_timeout", f.lastEvent(f.admin).getStr("reason"));
            assertTrue(f.owners.isEmpty());
            f.start(node, f.admin, "permission", null);
            doThrow(new IllegalStateException("revoked")).when(f.permissions).requireAccess(anyString(), anyString());
            node.expireSessions();
            assertEquals("permission_lost", f.lastEvent(f.admin).getStr("reason"));
        }
    }

    private static final class Fixture {
        private final List<RemoteLogWebSocketService> nodes = new ArrayList<>();
        private final Map<String, String> owners = new HashMap<>();
        private final Map<WsClientInfoEntity, List<JSONObject>> messages = new HashMap<>();
        private final RemoteClientBiz permissions = mock(RemoteClientBiz.class);
        private final WsClientPresenceStore presence = mock(WsClientPresenceStore.class);
        private final RedissonClient redisson = mock(RedissonClient.class);
        private final RTopic topic = mock(RTopic.class);
        @SuppressWarnings("unchecked")
        private final RMapCache<String, String> leaseMap = mock(RMapCache.class);
        private final WsClientInfoEntity admin = client("admin-1", "admin-user");
        private final WsClientInfoEntity app = client("app-1", "app-user");

        private Fixture() {
            WsClientPresenceRecord record = new WsClientPresenceRecord();
            record.setSessionId("app-1");
            record.setUserId("app-user");
            record.setLastSeenAt(System.currentTimeMillis());
            when(presence.get("app-1")).thenReturn(record);
            RLock lock = mock(RLock.class);
            when(lock.isHeldByCurrentThread()).thenReturn(true);
            when(redisson.getLock(anyString())).thenReturn(lock);
            when(leaseMap.get(anyString())).thenAnswer(call -> owners.get(call.getArgument(0)));
            when(leaseMap.fastPut(anyString(), anyString(), anyLong(), eq(TimeUnit.SECONDS))).thenAnswer(call -> {
                owners.put(call.getArgument(0), call.getArgument(1));
                return true;
            });
            when(leaseMap.remove(anyString(), anyString())).thenAnswer(call -> owners.remove(call.getArgument(0), call.getArgument(1)));
            when(topic.publish(anyString())).thenAnswer(call -> {
                String value = call.getArgument(0);
                for (RemoteLogWebSocketService node : List.copyOf(nodes)) ReflectionTestUtils.invokeMethod(node, "onRoutedMessage", new JSONObject(value));
                return (long) nodes.size();
            });
        }

        private RemoteLogWebSocketService node() {
            RemoteLogWebSocketService node = new RemoteLogWebSocketService();
            ReflectionTestUtils.setField(node, "remoteClientBiz", permissions);
            ReflectionTestUtils.setField(node, "presenceStore", presence);
            ReflectionTestUtils.setField(node, "redisson", redisson);
            ReflectionTestUtils.setField(node, "prefix", "test");
            ReflectionTestUtils.setField(node, "topic", topic);
            ReflectionTestUtils.setField(node, "clientOwners", leaseMap);
            nodes.add(node);
            return node;
        }

        private WsClientInfoEntity client(String connectionId, String userId) {
            WsClientInfoEntity client = mock(WsClientInfoEntity.class);
            Session session = mock(Session.class);
            when(session.getId()).thenReturn(connectionId);
            when(session.isOpen()).thenReturn(true);
            when(client.getSession()).thenReturn(session);
            when(client.getConnectionId()).thenReturn(connectionId);
            User user = new User();
            user.setId(userId);
            when(client.getUser()).thenReturn(user);
            when(client.getToken()).thenReturn("test-token");
            messages.put(client, new ArrayList<>());
            doAnswer(call -> {
                messages.get(client).add(new JSONObject(call.getArgument(1, Object.class)));
                return null;
            }).when(client).sendMessage(anyString(), any());
            return client;
        }

        private void start(RemoteLogWebSocketService node, WsClientInfoEntity admin, String requestId, Long expiry) {
            JSONObject request = new JSONObject(Map.of("action", "start", "clientId", "app-1", "requestId", requestId));
            if (expiry != null) request.set("expiresAt", expiry);
            node.onMessage(admin, request);
        }

        private JSONObject event(String action, String sessionId) {
            return new JSONObject(Map.of("action", action, "sessionId", sessionId));
        }

        private JSONObject lastEvent(WsClientInfoEntity client) {
            List<JSONObject> events = messages.get(client);
            return events.get(events.size() - 1);
        }
    }
}
