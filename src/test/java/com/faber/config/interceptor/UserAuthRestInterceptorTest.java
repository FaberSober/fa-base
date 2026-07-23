package com.faber.config.interceptor;

import com.faber.api.base.admin.entity.User;
import com.faber.core.exception.auth.UserNoPermissionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserAuthRestInterceptorTest {

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
}
