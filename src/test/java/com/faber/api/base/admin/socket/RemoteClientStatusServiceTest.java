package com.faber.api.base.admin.socket;

import cn.hutool.json.JSONObject;
import com.faber.api.base.admin.biz.RemoteClientBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.config.websocket.WsClientPresenceRecord;
import com.faber.config.websocket.WsClientPresenceStore;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RemoteClientStatusServiceTest {
    @Test
    void tracksOnlyTheSelectedUserAndInstallationAcrossReconnects() {
        Fixture f = new Fixture();
        WsClientPresenceRecord old = f.record("old", "selected", System.currentTimeMillis() - 1000);
        WsClientPresenceRecord otherDevice = f.record("other-device", "another", System.currentTimeMillis());
        when(f.store.all()).thenReturn(Map.of("old", old, "other", otherDevice));
        f.watch("watch-1");
        assertEquals("old", f.last().getStr("clientId"));
        assertEquals("online", f.last().getStr("state"));
        WsClientPresenceRecord newer = f.record("new", "selected", System.currentTimeMillis());
        when(f.store.all()).thenReturn(Map.of("old", old, "new", newer, "other", otherDevice));
        f.service.refreshWatches();
        assertEquals("new", f.last().getStr("clientId"));
        when(f.store.all()).thenReturn(Map.of("other", otherDevice));
        f.service.refreshWatches();
        assertEquals("offline", f.last().getStr("state"));
        assertNotNull(f.last().getLong("lastSeenAt"));
    }

    @Test
    void distinguishesDelayedOfflineAndUnavailableSnapshots() {
        Fixture f = new Fixture();
        WsClientPresenceRecord record = f.record("target", "selected", System.currentTimeMillis() - 45_000);
        when(f.store.all()).thenReturn(Map.of("target", record));
        f.watch("watch-1");
        assertEquals("delayed", f.last().getStr("state"));
        record.setLastSeenAt(System.currentTimeMillis() - 65_000);
        f.service.refreshWatches();
        assertEquals("offline", f.last().getStr("state"));
        when(f.store.all()).thenThrow(new IllegalStateException("redis offline"));
        f.service.refreshWatches();
        assertEquals("unknown", f.last().getStr("state"));
    }

    @Test
    void oldUnsubscribeCannotRemoveANewWatchAndRevokedPermissionEndsIt() {
        Fixture f = new Fixture();
        when(f.store.all()).thenReturn(Map.of());
        f.watch("old-watch");
        f.watch("new-watch");
        f.service.onMessage(f.admin, new JSONObject(Map.of("action", "unwatch", "watchId", "old-watch")));
        f.service.refreshWatches();
        assertEquals("new-watch", f.last().getStr("watchId"));
        doThrow(new IllegalStateException("revoked")).when(f.permissions).requireAccess(anyString(), anyString());
        f.service.refreshWatches();
        assertEquals("error", f.last().getStr("action"));
        int count = f.messages.size();
        f.service.refreshWatches();
        assertEquals(count, f.messages.size());
    }

    private static class Fixture {
        final RemoteClientStatusService service = new RemoteClientStatusService();
        final WsClientPresenceStore store = mock(WsClientPresenceStore.class);
        final RemoteClientBiz permissions = mock(RemoteClientBiz.class);
        final WsClientInfoEntity admin = mock(WsClientInfoEntity.class);
        final List<JSONObject> messages = new ArrayList<>();

        Fixture() {
            Session session = mock(Session.class);
            when(session.getId()).thenReturn("admin");
            when(session.isOpen()).thenReturn(true);
            when(admin.getSession()).thenReturn(session);
            User user = new User();
            user.setId("admin-user");
            when(admin.getUser()).thenReturn(user);
            when(admin.getToken()).thenReturn("test-token");
            doAnswer(call -> { messages.add(new JSONObject(call.getArgument(1, Object.class))); return null; }).when(admin).sendMessage(anyString(), any());
            ReflectionTestUtils.setField(service, "remoteClientBiz", permissions);
            ReflectionTestUtils.setField(service, "presenceStore", store);
        }

        void watch(String watchId) {
            service.onMessage(admin, new JSONObject(Map.of("action", "watch", "watchId", watchId, "userId", "app-user", "clientInstanceId", "selected")));
        }

        WsClientPresenceRecord record(String id, String instance, long connectedAt) {
            WsClientPresenceRecord record = new WsClientPresenceRecord();
            record.setSessionId(id);
            record.setUserId("app-user");
            record.setClientInstanceId(instance);
            record.setConnectedAt(connectedAt);
            record.setLastSeenAt(connectedAt);
            return record;
        }

        JSONObject last() { return messages.get(messages.size() - 1); }
    }
}
