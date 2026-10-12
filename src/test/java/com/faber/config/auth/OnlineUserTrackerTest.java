package com.faber.config.auth;

import cn.dev33.satoken.stp.StpUtil;
import com.faber.api.base.admin.entity.User;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.utils.RequestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnlineUserTrackerTest {
    private OnlineUserTracker tracker;
    private OnlineUserStore store;
    private User user;
    private MockedStatic<StpUtil> stp;
    private MockedStatic<RequestUtils> request;

    @BeforeEach
    void setup() {
        tracker = new OnlineUserTracker();
        store = mock(OnlineUserStore.class);
        ReflectionTestUtils.setField(tracker, "store", store);
        ReflectionTestUtils.setField(tracker, "touchIntervalSeconds", 30L);
        user = new User();
        user.setId("2");
        user.setUsername("test");
        stp = mockStatic(StpUtil.class);
        stp.when(() -> StpUtil.getLoginIdByToken("secret")).thenReturn("2");
        stp.when(() -> StpUtil.getLoginDeviceByToken("secret")).thenReturn("web");
        stp.when(() -> StpUtil.getTokenTimeout("secret")).thenReturn(60L);
        request = mockStatic(RequestUtils.class);
        request.when(RequestUtils::getAgent).thenReturn("Mozilla/5.0");
    }

    @AfterEach
    void cleanup() {
        stp.close();
        request.close();
        BaseContextHandler.remove();
    }

    @Test
    void newLoginGetsNonCredentialIdAndMatchingTtl() {
        tracker.touch("secret", user, true);
        var capture = ArgumentCaptor.forClass(OnlineUserSession.class);
        verify(store).put(capture.capture(), eq(60L));
        var session = capture.getValue();
        assertEquals(64, session.getId().length());
        assertNotNull(session.getLoginTime());
        assertEquals(60_000, session.getExpiresAt() - session.getLastAccessTime());
        assertFalse(session.toString().contains("secret"));
    }

    @Test
    void existingSessionPreservesLoginTimeAndThrottlesActivityWrites() {
        OnlineUserSession previous = new OnlineUserSession();
        previous.setLoginTime(123L);
        previous.setLastAccessTime(System.currentTimeMillis());
        when(store.get(OnlineUserTracker.sessionId("secret"))).thenReturn(previous);
        tracker.touch("secret", user, false);
        verify(store, never()).put(any(), anyLong());
        tracker.touch("secret", user, true);
        var capture = ArgumentCaptor.forClass(OnlineUserSession.class);
        verify(store).put(capture.capture(), eq(60L));
        assertEquals(123L, capture.getValue().getLoginTime());
    }

    @Test
    void oldSessionHasUnknownLoginTimeAndPortalIsNotTracked() {
        tracker.touch("secret", user, false);
        var capture = ArgumentCaptor.forClass(OnlineUserSession.class);
        verify(store).put(capture.capture(), eq(60L));
        assertNull(capture.getValue().getLoginTime());
        clearInvocations(store);
        stp.when(() -> StpUtil.getLoginDeviceByToken("secret")).thenReturn("portal");
        tracker.touch("secret", user, true);
        verify(store, never()).put(any(), anyLong());
    }

    @Test
    void expiredTokenCannotBeRegisteredAndStoreFailureDoesNotBreakLogin() {
        stp.when(() -> StpUtil.getTokenTimeout("secret")).thenReturn(-2L);
        tracker.touch("secret", user, true);
        verify(store, never()).put(any(), anyLong());
        when(store.get(anyString())).thenThrow(new IllegalStateException("unavailable"));
        assertDoesNotThrow(() -> tracker.touch("secret", user, true));
    }
}
