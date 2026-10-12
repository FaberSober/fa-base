package com.faber.config.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.core.config.annotation.AdminOpr;
import com.faber.core.exception.auth.UserNoPermissionException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAuthRestInterceptorTest {

    @Test
    void adminOperationQueriesUserWithStringId() throws Exception {
        UserBiz userBiz = mock(UserBiz.class);
        User admin = new User();
        admin.setId("1");
        when(userBiz.getById("1")).thenReturn(admin);
        UserAuthRestInterceptor interceptor = new UserAuthRestInterceptor();
        ReflectionTestUtils.setField(interceptor, "userBiz", userBiz);
        HandlerMethod handler = new HandlerMethod(new AdminController(), "save");

        assertTrue(interceptor.preHandle(
                new MockHttpServletRequest(), new MockHttpServletResponse(), handler
        ));
        verify(userBiz).getById("1");
        verify(userBiz).setUserLogin(admin);
    }

    static class AdminController {
        @AdminOpr
        public void save() {
        }
    }

    @Test
    void onlyPortalApiNamespaceSkipsAdminQualification() {
        assertFalse(UserAuthRestInterceptor.requiresAdminAccess("/api/portal"));
        assertFalse(UserAuthRestInterceptor.requiresAdminAccess("/api/portal/account/me"));
        assertTrue(UserAuthRestInterceptor.requiresAdminAccess("/api/portal-admin"));
        assertTrue(UserAuthRestInterceptor.requiresAdminAccess("/api/base/admin/user/page"));
    }

    @Test
    void disablingAdminQualificationBlocksNextAdminRequestButKeepsPortalAccess() {
        User user = new User();
        user.setAdminEnabled(true);
        assertDoesNotThrow(() ->
                UserAuthRestInterceptor.requireApplicationAccess("/api/base/admin/user/page", user)
        );

        user.setAdminEnabled(false);
        assertThrows(
                UserNoPermissionException.class,
                () -> UserAuthRestInterceptor.requireApplicationAccess("/api/base/admin/user/page", user)
        );
        assertDoesNotThrow(() ->
                UserAuthRestInterceptor.requireApplicationAccess("/api/portal/account/me", user)
        );
    }

    @Test
    void invalidTokenReturnsUnauthorizedResponseWithoutThrowing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean proceed;
        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getTokenValue).thenReturn("");
            proceed = new UserAuthRestInterceptor().preHandle(
                    new MockHttpServletRequest(), response, new Object()
            );
        }

        assertFalse(proceed);
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":40101"));
    }
}
