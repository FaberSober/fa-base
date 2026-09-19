package com.faber.api.base.rbac.biz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.api.base.rbac.enums.RbacRoleTypeEnum;
import com.faber.api.base.rbac.mapper.RbacRoleMapper;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RbacRoleBizTest {

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
        TenantContext.clear();
    }

    @Test
    void disabledTenantModeKeepsGlobalRoleManagementAvailable() {
        RbacRoleBiz biz = createBiz(false);
        BaseContextHandler.setUserId("user-1");

        assertDoesNotThrow(() -> biz.checkCanManageRole(new RbacRole()));
    }

    @Test
    void disabledTenantModeDoesNotAddRoleScopeFilter() {
        RbacRoleBiz biz = createBiz(false);
        QueryWrapper<RbacRole> wrapper = new QueryWrapper<>();

        ReflectionTestUtils.invokeMethod(biz, "appendRoleScopeQuery", wrapper);

        assertTrue(wrapper.getExpression().getNormal().isEmpty());
    }

    @Test
    void doesNotDuplicateExistingTenantAdminRole() {
        RbacRoleMapper mapper = mock(RbacRoleMapper.class);
        RbacRoleBiz biz = new RbacRoleBiz();
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);

        RbacRole role = new RbacRole();
        role.setType(RbacRoleTypeEnum.TENANT);
        role.setTenantId("tenant-1");
        when(mapper.selectOne(any())).thenReturn(role);

        biz.ensureTenantAdminRole("tenant-1");

        verify(mapper, never()).insert(any(RbacRole.class));
    }

    @Test
    void createsAndReturnsTenantAdminRole() {
        RbacRoleMapper mapper = mock(RbacRoleMapper.class);
        RbacRoleBiz biz = new RbacRoleBiz();
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);
        when(mapper.selectOne(any())).thenReturn(null);
        when(mapper.insert(any(RbacRole.class))).thenAnswer(invocation -> {
            RbacRole role = invocation.getArgument(0);
            role.setId(10L);
            return 1;
        });

        RbacRole role = biz.ensureTenantAdminRole("tenant-1");

        assertEquals(10L, role.getId());
        verify(mapper).insert(any(RbacRole.class));
    }

    @Test
    void tenantUserCannotReadAnotherTenantRoleById() {
        RbacRoleMapper mapper = mock(RbacRoleMapper.class);
        RbacRoleBiz biz = createBiz(true);
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);
        BaseContextHandler.setUserId("user-1");
        TenantContext.setTenantId("tenant-1");

        RbacRole role = new RbacRole();
        role.setId(2L);
        role.setType(RbacRoleTypeEnum.TENANT);
        role.setTenantId("tenant-2");
        when(mapper.selectById(2L)).thenReturn(role);

        assertThrows(BuzzException.class, () -> biz.getById(2L));
    }

    @Test
    void tenantAdminCannotAssignGlobalRole() {
        RbacRoleBiz biz = createBiz(true);
        ReflectionTestUtils.setField(biz, "tenantUserBiz", mock(TenantUserBiz.class));
        BaseContextHandler.setUserId("user-1");
        TenantContext.setTenantId("tenant-1");

        RbacRole role = new RbacRole();
        role.setType(RbacRoleTypeEnum.GLOBAL);

        assertThrows(BuzzException.class, () -> biz.checkCanAssignRole(role));
    }

    @Test
    void tenantAdminCanAssignCurrentTenantRole() {
        TenantUserBiz tenantUserBiz = mock(TenantUserBiz.class);
        RbacRoleBiz biz = createBiz(true);
        ReflectionTestUtils.setField(biz, "tenantUserBiz", tenantUserBiz);
        BaseContextHandler.setUserId("user-1");
        TenantContext.setTenantId("tenant-1");
        when(tenantUserBiz.isTenantAdminUser("user-1", "tenant-1")).thenReturn(true);

        RbacRole role = new RbacRole();
        role.setType(RbacRoleTypeEnum.TENANT);
        role.setTenantId("tenant-1");
        role.setName("业务角色");

        assertDoesNotThrow(() -> biz.checkCanAssignRole(role));
    }

    private RbacRoleBiz createBiz(boolean tenantEnabled) {
        FaSetting faSetting = new FaSetting();
        FaSetting.Tenant tenant = new FaSetting.Tenant();
        tenant.setEnabled(tenantEnabled);
        faSetting.setTenant(tenant);

        RbacRoleBiz biz = new RbacRoleBiz();
        ReflectionTestUtils.setField(biz, "faSetting", faSetting);
        return biz;
    }
}
