package com.faber.api.base.tn.biz;

import com.faber.api.base.rbac.biz.RbacRoleBiz;
import com.faber.api.base.rbac.biz.RbacRoleMenuBiz;
import com.faber.api.base.rbac.biz.RbacUserRoleBiz;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantBizTest {

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
    }

    @Test
    void syncsTenantRoleAndCreatorMembershipWhenTenantModeIsEnabled() {
        TenantBiz biz = createBiz(true);
        RbacRoleBiz roleBiz = mock(RbacRoleBiz.class);
        RbacRoleMenuBiz roleMenuBiz = mock(RbacRoleMenuBiz.class);
        RbacUserRoleBiz userRoleBiz = mock(RbacUserRoleBiz.class);
        TenantPermissionBiz permissionBiz = mock(TenantPermissionBiz.class);
        TenantUserBiz tenantUserBiz = mock(TenantUserBiz.class);
        ReflectionTestUtils.setField(biz, "rbacRoleBiz", roleBiz);
        ReflectionTestUtils.setField(biz, "rbacRoleMenuBiz", roleMenuBiz);
        ReflectionTestUtils.setField(biz, "rbacUserRoleBiz", userRoleBiz);
        ReflectionTestUtils.setField(biz, "tenantPermissionBiz", permissionBiz);
        ReflectionTestUtils.setField(biz, "tenantUserBiz", tenantUserBiz);
        BaseContextHandler.setUserId("user-1");

        Tenant tenant = new Tenant();
        tenant.setId("tenant-1");
        RbacRole tenantAdminRole = new RbacRole();
        tenantAdminRole.setId(10L);
        when(roleBiz.ensureTenantAdminRole("tenant-1")).thenReturn(tenantAdminRole);
        when(permissionBiz.getAllowedMenuIds("tenant-1")).thenReturn(List.of(100L));

        biz.syncTenantLifecycle(tenant, true, List.of(100L));

        verify(roleBiz).ensureTenantAdminRole("tenant-1");
        verify(permissionBiz).initializePermissions("tenant-1", List.of(100L));
        verify(roleMenuBiz).ensureRoleMenus(10L, List.of(100L));
        verify(tenantUserBiz).ensureTenantAdmin("tenant-1", "user-1");
        verify(userRoleBiz).ensureUserRole("user-1", 10L);
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

        biz.syncTenantLifecycle(tenant, true, List.of(100L));

        verify(roleBiz, never()).ensureTenantAdminRole("tenant-1");
        verify(tenantUserBiz, never()).ensureTenantAdmin("tenant-1", null);
    }

    @Test
    void blocksGenericSaveOrUpdateWhenTenantModeIsEnabled() {
        TenantBiz biz = createBiz(true);

        assertThrows(BuzzException.class, () -> biz.saveOrUpdate(new Tenant()));
        assertThrows(BuzzException.class, () -> biz.saveOrUpdateBatch(List.of(new Tenant())));
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
