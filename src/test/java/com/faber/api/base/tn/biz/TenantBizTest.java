package com.faber.api.base.tn.biz;

import com.faber.api.base.rbac.biz.RbacRoleBiz;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TenantBizTest {

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
    }

    @Test
    void syncsTenantRoleAndCreatorMembershipWhenTenantModeIsEnabled() {
        TenantBiz biz = createBiz(true);
        RbacRoleBiz roleBiz = mock(RbacRoleBiz.class);
        TenantUserBiz tenantUserBiz = mock(TenantUserBiz.class);
        ReflectionTestUtils.setField(biz, "rbacRoleBiz", roleBiz);
        ReflectionTestUtils.setField(biz, "tenantUserBiz", tenantUserBiz);
        BaseContextHandler.setUserId("user-1");

        Tenant tenant = new Tenant();
        tenant.setId("tenant-1");

        biz.syncTenantLifecycle(tenant, true);

        verify(roleBiz).ensureTenantAdminRole("tenant-1");
        verify(tenantUserBiz).ensureTenantAdmin("tenant-1", "user-1");
    }

    @Test
    void skipsTenantLifecycleSyncWhenTenantModeIsDisabled() {
        TenantBiz biz = createBiz(false);
        RbacRoleBiz roleBiz = mock(RbacRoleBiz.class);
        TenantUserBiz tenantUserBiz = mock(TenantUserBiz.class);
        ReflectionTestUtils.setField(biz, "rbacRoleBiz", roleBiz);
        ReflectionTestUtils.setField(biz, "tenantUserBiz", tenantUserBiz);

        Tenant tenant = new Tenant();
        tenant.setId("tenant-1");

        biz.syncTenantLifecycle(tenant, true);

        verify(roleBiz, never()).ensureTenantAdminRole("tenant-1");
        verify(tenantUserBiz, never()).ensureTenantAdmin("tenant-1", null);
    }

    private TenantBiz createBiz(boolean tenantEnabled) {
        FaSetting faSetting = new FaSetting();
        FaSetting.Tenant tenant = new FaSetting.Tenant();
        tenant.setEnabled(tenantEnabled);
        faSetting.setTenant(tenant);

        TenantBiz biz = new TenantBiz();
        ReflectionTestUtils.setField(biz, "faSetting", faSetting);
        return biz;
    }
}
