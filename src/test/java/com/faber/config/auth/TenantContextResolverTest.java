package com.faber.config.auth;

import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.auth.UserTokenException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TenantContextResolverTest {

    private final TenantUserBiz tenantUserBiz = mock(TenantUserBiz.class);
    private final FaSetting faSetting = new FaSetting();
    private final TenantContextResolver resolver = new TenantContextResolver();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resolver, "tenantUserBiz", tenantUserBiz);
        ReflectionTestUtils.setField(resolver, "faSetting", faSetting);
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
    }

    @Test
    void disabledTenantModeClearsContextAndIgnoresHeader() {
        enableTenant(false);
        TenantContext.setTenantId("stale-tenant");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("fa-tn-tenant-id", "request-tenant");

        resolver.resolve(request, "user-1");

        assertNull(TenantContext.getTenantId());
    }

    @Test
    void enabledTenantModeUsesRequestedTenant() {
        enableTenant(true);
        when(tenantUserBiz.hasUserTenant("user-1", "tenant-2")).thenReturn(true);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("fa-tn-tenant-id", "tenant-2");

        resolver.resolve(request, "user-1");

        assertEquals("tenant-2", TenantContext.getTenantId());
    }

    @Test
    void enabledTenantModeRejectsUserWithoutDefaultTenant() {
        enableTenant(true);
        when(tenantUserBiz.getDefaultTenantId("user-1")).thenReturn(null);

        assertThrows(UserTokenException.class,
                () -> resolver.resolve(new MockHttpServletRequest(), "user-1"));
    }

    private void enableTenant(boolean enabled) {
        FaSetting.Tenant tenant = new FaSetting.Tenant();
        tenant.setEnabled(enabled);
        faSetting.setTenant(tenant);
    }
}
