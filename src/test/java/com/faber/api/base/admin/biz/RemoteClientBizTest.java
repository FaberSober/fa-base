package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.vo.ret.RemoteClientVo;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoteClientBizTest {

    @Test
    void defaultOrderIgnoresHeartbeatsAndBreaksConnectionTimeTies() {
        RemoteClientVo older = client("c", "alice", 100, 900);
        RemoteClientVo first = client("a", "bob", 200, 300);
        RemoteClientVo second = client("b", "bob", 200, 400);
        List<RemoteClientVo> clients = List.of(second, older, first);
        assertEquals(List.of("a", "b", "c"), sortedIds(clients, null));
        older.setLastSeenAt(1_000);
        first.setLastSeenAt(2_000);
        assertEquals(List.of("a", "b", "c"), sortedIds(List.of(older, second, first), null));
        assertEquals(List.of("a", "b", "c"), sortedIds(clients, "unsupported DESC"));
    }

    @Test
    void accountSortSupportsBothDirectionsAndStableTies() {
        List<RemoteClientVo> clients = List.of(
                client("c", "Bob", 100, 900),
                client("a", "alice", 200, 300),
                client("b", "bob", 200, 400));
        assertEquals(List.of("a", "b", "c"), sortedIds(clients, "username ASC"));
        assertEquals(List.of("b", "c", "a"), sortedIds(clients, "username DESC"));
    }

    @Test
    void optionalTextFieldsCanBeMissing() {
        RemoteClientVo missing = client("a", "alice", 200, 300);
        RemoteClientVo named = client("b", "bob", 100, 400);
        named.setAppName("Mobile");
        assertEquals(List.of("b", "a"), sortedIds(List.of(missing, named), "app_name ASC"));
    }

    @Test
    void timestampsSortNumericallyAndHeartbeatSortingIsExplicit() {
        List<RemoteClientVo> clients = List.of(
                client("a", "alice", 20, 100),
                client("b", "bob", 100, 20));
        assertEquals(List.of("a", "b"), sortedIds(clients, "connected_at ASC"));
        assertEquals(List.of("b", "a"), sortedIds(clients, "connected_at DESC"));
        assertEquals(List.of("b", "a"), sortedIds(clients, "last_seen_at ASC"));
        assertEquals(List.of("a", "b"), sortedIds(clients, "last_seen_at DESC"));
    }

    private RemoteClientVo client(String id, String username, long connectedAt, long lastSeenAt) {
        RemoteClientVo client = new RemoteClientVo();
        client.setId(id);
        client.setUsername(username);
        client.setConnectedAt(connectedAt);
        client.setLastSeenAt(lastSeenAt);
        return client;
    }

    private List<String> sortedIds(List<RemoteClientVo> clients, String sorter) {
        Comparator<RemoteClientVo> order = ReflectionTestUtils.invokeMethod(new RemoteClientBiz(), "buildComparator", sorter);
        return clients.stream().sorted(order).map(RemoteClientVo::getId).toList();
    }
}
