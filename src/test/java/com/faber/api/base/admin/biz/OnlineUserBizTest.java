package com.faber.api.base.admin.biz;

import cn.dev33.satoken.stp.StpUtil;
import com.faber.api.base.admin.vo.query.OnlineUserKickoutVo;
import com.faber.api.base.admin.vo.query.OnlineUserQueryVo;
import com.faber.api.base.rbac.mapper.RbacUserRoleMapper;
import com.faber.config.auth.OnlineUserSession;
import com.faber.config.auth.OnlineUserStore;
import com.faber.config.auth.OnlineUserTracker;
import com.faber.config.websocket.WsClientPresenceRecord;
import com.faber.config.websocket.WsClientPresenceStore;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.auth.UserNoPermissionException;
import com.faber.core.exception.auth.UserTokenException;
import com.faber.core.vo.query.BasePageQuery;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnlineUserBizTest {
    private OnlineUserBiz biz;
    private OnlineUserStore store;
    private WsClientPresenceStore clientPresenceStore;
    private RbacUserRoleMapper roles;
    private MockedStatic<StpUtil> stp;

    @BeforeEach
    void setup() {
        biz = new OnlineUserBiz();
        store = mock(OnlineUserStore.class);
        clientPresenceStore = mock(WsClientPresenceStore.class);
        roles = mock(RbacUserRoleMapper.class);
        ReflectionTestUtils.setField(biz, "store", store);
        ReflectionTestUtils.setField(biz, "clientPresenceStore", clientPresenceStore);
        ReflectionTestUtils.setField(biz, "roleMapper", roles);
        ReflectionTestUtils.setField(biz, "activeWindowSeconds", 300L);
        BaseContextHandler.setUserId("1");
        stp = mockStatic(StpUtil.class);
        stp.when(StpUtil::getTokenValue).thenReturn("current-secret");
        stp.when(() -> StpUtil.getLoginIdByToken("current-secret")).thenReturn("1");
        stp.when(() -> StpUtil.getLoginDeviceByToken("current-secret")).thenReturn("web");
    }

    @AfterEach
    void cleanup() {
        stp.close();
        BaseContextHandler.remove();
    }

    private OnlineUserSession session(String token, String userId, long ago) {
        OnlineUserSession session = new OnlineUserSession();
        session.setId(OnlineUserTracker.sessionId(token));
        session.setToken(token);
        session.setUserId(userId);
        session.setUsername("account-" + userId);
        session.setName("用户" + userId);
        session.setLastAccessTime(System.currentTimeMillis() - ago);
        session.setExpiresAt(System.currentTimeMillis() + 60_000);
        stp.when(() -> StpUtil.getLoginIdByToken(token)).thenReturn(userId);
        stp.when(() -> StpUtil.getLoginDeviceByToken(token)).thenReturn("web");
        when(store.get(session.getId())).thenReturn(session);
        return session;
    }

    private OnlineUserKickoutVo kick(OnlineUserSession session, boolean all) {
        OnlineUserKickoutVo params = new OnlineUserKickoutVo();
        params.setId(session.getId());
        params.setAllSessions(all);
        return params;
    }

    private WsClientPresenceRecord presenceRecord(
            String sessionId, String clientInstanceId, long connectedAt, long lastSeenAt) {
        WsClientPresenceRecord record = new WsClientPresenceRecord();
        record.setSessionId(sessionId);
        record.setUserId("2");
        record.setClientInstanceId(clientInstanceId);
        record.setClientType("MOBILE");
        record.setConnectedAt(connectedAt);
        record.setLastSeenAt(lastSeenAt);
        return record;
    }

    @Test
    void pageFiltersAndStatsDeduplicateUsersWithoutExposingCredentials() throws Exception {
        var active = session("secret-a", "2", 1000);
        var idle = session("secret-b", "2", 400_000);
        var expired = session("expired-secret", "3", 1000);
        expired.setExpiresAt(1L);
        var portal = session("portal-secret", "4", 1000);
        stp.when(() -> StpUtil.getLoginDeviceByToken("portal-secret")).thenReturn("portal");
        when(store.all()).thenReturn(List.of(active, idle, expired, portal));
        BasePageQuery<OnlineUserQueryVo> query = new BasePageQuery<>();
        OnlineUserQueryVo filter = new OnlineUserQueryVo();
        filter.setKeyword("ACCOUNT-2");
        filter.setActive(false);
        query.setQuery(filter);
        query.setCurrent(99);
        var page = biz.page(query);
        assertEquals(1, page.getData().getTotal());
        assertEquals(1, page.getData().getPagination().getCurrent());
        assertEquals(idle.getId(), page.getData().getRows().get(0).getId());
        String json = new ObjectMapper().writeValueAsString(page);
        assertFalse(json.contains("secret"));
        assertFalse(json.contains("\"token\""));
        var stats = biz.stats();
        assertEquals(2, stats.getSessionCount());
        assertEquals(1, stats.getUserCount());
        assertEquals(1, stats.getActiveUserCount());
        verify(store).remove(expired.getId());
        verify(store).remove(portal.getId());
        verify(store, times(1)).all(); // 分页、统计复用快照。
    }

    @Test
    void duplicateConnectionsCountAsOneDeviceUntilLastConnectionCloses() {
        long now = System.currentTimeMillis();
        var firstConnection = presenceRecord("socket-a", "mobile-install-2", now - 5_000, now - 1_000);
        var secondConnection = presenceRecord("socket-b", "mobile-install-2", now - 4_000, now - 500);
        when(clientPresenceStore.all()).thenReturn(
                Map.of("socket-a", firstConnection, "socket-b", secondConnection),
                Map.of("socket-b", secondConnection),
                Map.of());

        var bothConnections = biz.presenceDevices("2");
        assertEquals(1, bothConnections.size());
        assertEquals(now - 5_000, bothConnections.get(0).getConnectedAt());
        assertEquals(now - 500, bothConnections.get(0).getLastSeenAt());

        var oneConnection = biz.presenceDevices("2");
        assertEquals(1, oneConnection.size());
        assertEquals(now - 4_000, oneConnection.get(0).getConnectedAt());

        assertTrue(biz.presenceDevices("2").isEmpty());
    }

    @Test
    void presenceExcludesConnectionsWithoutAHeartbeatForSixtySeconds() {
        long now = System.currentTimeMillis();
        long window = WsClientPresenceStore.ONLINE_TTL_SECONDS * 1000;
        var expired = presenceRecord("socket-expired", "mobile-install-expired",
                now - window - 1_000, now - window - 1_000);
        expired.setDeviceModel("expired-device");
        var active = presenceRecord("socket-active", "mobile-install-active",
                now - window + 1_000, now - window + 1_000);
        active.setDeviceModel("active-device");
        when(clientPresenceStore.all()).thenReturn(
                Map.of("socket-expired", expired, "socket-active", active));

        var devices = biz.presenceDevices("2");

        assertEquals(1, devices.size());
        assertEquals("active-device", devices.get(0).getDeviceModel());
    }

    @Test
    void cannotKickCurrentSharedSessionOrAllOwnSessions() {
        var current = session("current-secret", "1", 0);
        assertThrows(BuzzException.class, () -> biz.kickout(kick(current, false)));
        var another = session("another-own", "1", 0);
        assertThrows(BuzzException.class, () -> biz.kickout(kick(another, true)));
        stp.verify(() -> StpUtil.kickoutByTokenValue(anyString()), never());
    }

    @Test
    void singleKickDoesNotRevokeOtherSessionsAndIsIdempotent() {
        var target = session("target-secret", "2", 0);
        assertEquals(1, biz.kickout(kick(target, false)));
        stp.verify(() -> StpUtil.kickoutByTokenValue("target-secret"));
        stp.verify(() -> StpUtil.getTokenValueListByLoginId(any(), anyString()), never());
        verify(store).remove(target.getId());
        assertFalse(BaseContextHandler.getLogOprRemark().contains("target-secret"));
        assertTrue(BaseContextHandler.getLogOprRemark().contains("目标用户=2"));
        when(store.get(target.getId())).thenReturn(null);
        assertEquals(0, biz.kickout(kick(target, false)));
        stp.verify(() -> StpUtil.kickoutByTokenValue("target-secret"), times(1));
    }

    @Test
    void allKickOnlyUsesWebTokensIncludingUnindexedSessions() {
        var target = session("web-a", "2", 0);
        stp.when(() -> StpUtil.getTokenValueListByLoginId("2", "web")).thenReturn(List.of("web-a", "web-b"));
        assertEquals(2, biz.kickout(kick(target, true)));
        stp.verify(() -> StpUtil.kickoutByTokenValue("web-a"));
        stp.verify(() -> StpUtil.kickoutByTokenValue("web-b"));
        stp.verify(() -> StpUtil.kickoutByTokenValue("portal-secret"), never());
    }

    @Test
    void kickoutUserSessionsUsesServerSideWebTokensAndRejectsCurrentUser() {
        stp.when(() -> StpUtil.getTokenValueListByLoginId("2", "web")).thenReturn(List.of("web-a", "web-b"));
        assertEquals(2, biz.kickoutUserSessions("2"));
        stp.verify(() -> StpUtil.kickoutByTokenValue("web-a"));
        stp.verify(() -> StpUtil.kickoutByTokenValue("web-b"));
        verify(store).remove(OnlineUserTracker.sessionId("web-a"));
        verify(store).remove(OnlineUserTracker.sessionId("web-b"));
        assertFalse(BaseContextHandler.getLogOprRemark().contains("web-a"));
        assertThrows(BuzzException.class, () -> biz.kickoutUserSessions("1"));
        stp.verify(() -> StpUtil.getTokenValueListByLoginId("1", "web"), never());
    }

    @Test
    void rejectsTenantOnlyRoleAndSeparatesViewAndKickPermissions() {
        BaseContextHandler.setUserId("5");
        stp.when(() -> StpUtil.getLoginIdByToken("current-secret")).thenReturn("5");
        assertThrows(UserNoPermissionException.class, () -> biz.page(new BasePageQuery<>()));
        verifyNoInteractions(store);
        when(roles.countPlatformPermission("5", OnlineUserBiz.VIEW_PERMISSION)).thenReturn(1);
        when(store.all()).thenReturn(List.of());
        assertDoesNotThrow(() -> biz.page(new BasePageQuery<>()));
        var target = session("target-secret", "2", 0);
        assertThrows(UserNoPermissionException.class, () -> biz.kickout(kick(target, false)));
        assertThrows(UserNoPermissionException.class, () -> biz.kickoutUserSessions("2"));
        verify(store, never()).get(target.getId());
        when(roles.countPlatformPermission("5", OnlineUserBiz.KICK_PERMISSION)).thenReturn(1);
        assertEquals(1, biz.kickout(kick(target, false)));
        stp.when(() -> StpUtil.getTokenValueListByLoginId("2", "web")).thenReturn(List.of("target-secret"));
        assertEquals(1, biz.kickoutUserSessions("2"));
    }

    @Test
    void rejectsPortalAndApiTokenEvenForSuperAdmin() {
        stp.when(() -> StpUtil.getLoginDeviceByToken("current-secret")).thenReturn("portal");
        assertThrows(UserNoPermissionException.class, biz::stats);
        stp.when(() -> StpUtil.getLoginIdByToken("current-secret")).thenReturn(null);
        assertThrows(UserNoPermissionException.class, biz::stats);
        verifyNoInteractions(store);
    }

    @Test
    void revisionChangeRefreshesOtherNodeSnapshot() {
        var target = session("target-secret", "2", 0);
        when(store.all()).thenReturn(List.of(target), List.of());
        when(store.revision()).thenReturn(0L, 1L);
        assertEquals(1, biz.stats().getSessionCount());
        assertEquals(0, biz.stats().getSessionCount());
    }

    @Test
    void missingLegacyDeviceRequiresReloginInsteadOfReportingMissingPermission() {
        stp.when(() -> StpUtil.getLoginDeviceByToken("current-secret")).thenReturn(null);
        UserTokenException error = assertThrows(UserTokenException.class, biz::stats);
        assertTrue(error.getMessage().contains("重新登录"));
        verifyNoInteractions(store, roles);
    }
}
