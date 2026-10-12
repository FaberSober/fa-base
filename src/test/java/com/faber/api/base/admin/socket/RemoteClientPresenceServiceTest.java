package com.faber.api.base.admin.socket;

import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.config.websocket.WsClientPresenceStore;
import jakarta.websocket.Session;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class RemoteClientPresenceServiceTest {

    @Test
    void closeRemovesOnlyTheClosedConnectionRecord() {
        WsClientPresenceStore store = mock(WsClientPresenceStore.class);
        RemoteClientPresenceService service = new RemoteClientPresenceService();
        ReflectionTestUtils.setField(service, "presenceStore", store);
        Session session = mock(Session.class);
        when(session.getId()).thenReturn("socket-a");
        WsClientInfoEntity client = new WsClientInfoEntity();
        client.setSession(session);

        service.onClose(client);

        verify(store).remove("socket-a");
    }
}
