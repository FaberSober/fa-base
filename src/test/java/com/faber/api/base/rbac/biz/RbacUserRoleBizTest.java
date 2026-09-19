package com.faber.api.base.rbac.biz;

import com.faber.api.base.rbac.mapper.RbacUserRoleMapper;
import com.faber.api.base.rbac.entity.RbacUserRole;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.api.base.rbac.enums.RbacRoleTypeEnum;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RbacUserRoleBizTest {

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
        TenantContext.clear();
    }

    @Test
    void permissionCheckUsesCurrentTenant() {
        RbacUserRoleMapper mapper = mock(RbacUserRoleMapper.class);
        RbacUserRoleBiz biz = createBiz(mapper, true);
        BaseContextHandler.setUserId("user-1");
        TenantContext.setTenantId("tenant-1");
        when(mapper.countByUserIdAndLinkUrl("user-1", "/demo", "tenant-1")).thenReturn(1);

        assertTrue(biz.checkUserLinkUrl("user-1", "/demo"));
        verify(mapper).countByUserIdAndLinkUrl("user-1", "/demo", "tenant-1");
    }

    @Test
    void permissionCacheSeparatesTenants() {
        RbacUserRoleMapper mapper = mock(RbacUserRoleMapper.class);
        RbacUserRoleBiz biz = createBiz(mapper, true);
        BaseContextHandler.setUserId("user-1");
        TenantContext.setTenantId("tenant-1");
        when(mapper.countByUserIdAndLinkUrl("user-1", "/demo", "tenant-1")).thenReturn(1);
        when(mapper.countByUserIdAndLinkUrl("user-1", "/demo", "tenant-2")).thenReturn(0);

        assertTrue(biz.checkUserLinkUrl("user-1", "/demo"));
        assertTrue(biz.checkUserLinkUrl("user-1", "/demo"));

        TenantContext.setTenantId("tenant-2");
        assertFalse(biz.checkUserLinkUrl("user-1", "/demo"));

        verify(mapper, times(1)).countByUserIdAndLinkUrl("user-1", "/demo", "tenant-1");
        verify(mapper, times(1)).countByUserIdAndLinkUrl("user-1", "/demo", "tenant-2");
    }

    @Test
    void permissionCheckRejectsMissingTenantContext() {
        RbacUserRoleMapper mapper = mock(RbacUserRoleMapper.class);
        RbacUserRoleBiz biz = createBiz(mapper, true);
        BaseContextHandler.setUserId("user-1");

        assertFalse(biz.checkUserLinkUrl("user-1", "/demo"));
        verifyNoInteractions(mapper);
    }

    @Test
    void initializesUserRoleBindingOnlyWhenMissing() {
        RbacUserRoleMapper mapper = mock(RbacUserRoleMapper.class);
        RbacUserRoleBiz biz = createBiz(mapper, true);
        when(mapper.selectCount(any())).thenReturn(0L);

        biz.ensureUserRole("user-1", 10L);

        ArgumentCaptor<RbacUserRole> captor = ArgumentCaptor.forClass(RbacUserRole.class);
        verify(mapper).insert(captor.capture());
        assertEquals("user-1", captor.getValue().getUserId());
        assertEquals(10L, captor.getValue().getRoleId());
    }

    @Test
    void tenantRoleChangePreservesBindingsOutsideCurrentTenant() {
        RbacUserRoleMapper mapper = mock(RbacUserRoleMapper.class);
        RbacRoleBiz roleBiz = mock(RbacRoleBiz.class);
        TenantUserBiz tenantUserBiz = mock(TenantUserBiz.class);
        RbacUserRoleBiz biz = createBiz(mapper, true);
        ReflectionTestUtils.setField(biz, "rbacRoleBiz", roleBiz);
        ReflectionTestUtils.setField(biz, "tenantUserBiz", tenantUserBiz);
        BaseContextHandler.setUserId("tenant-admin");
        TenantContext.setTenantId("tenant-1");

        RbacUserRole currentTenantBinding = binding(11L, "user-1", 101L);
        RbacUserRole otherTenantBinding = binding(12L, "user-1", 202L);
        when(mapper.selectList(any())).thenReturn(java.util.List.of(currentTenantBinding, otherTenantBinding));

        RbacRole currentTenantRole = role(101L, "tenant-1");
        RbacRole otherTenantRole = role(202L, "tenant-2");
        RbacRole replacementRole = role(303L, "tenant-1");
        when(roleBiz.getRoleForBinding(101L)).thenReturn(currentTenantRole);
        when(roleBiz.getRoleForBinding(202L)).thenReturn(otherTenantRole);
        when(roleBiz.getRoleForBinding(303L)).thenReturn(replacementRole);
        when(roleBiz.isTenantRole(any())).thenReturn(true);
        when(roleBiz.isTenantAdminRole(any())).thenReturn(false);
        when(roleBiz.isRoleInTenantScope(any(), eq("tenant-1"))).thenAnswer(invocation -> {
            RbacRole role = invocation.getArgument(0);
            return "tenant-1".equals(role.getTenantId());
        });
        when(tenantUserBiz.hasUserTenant("user-1", "tenant-1")).thenReturn(true);

        biz.changeUserRoles("user-1", java.util.List.of(303L));

        verify(mapper).deleteById(11L);
        verify(mapper, never()).deleteById(12L);
        ArgumentCaptor<RbacUserRole> captor = ArgumentCaptor.forClass(RbacUserRole.class);
        verify(mapper).insert(captor.capture());
        assertEquals(303L, captor.getValue().getRoleId());
    }

    @Test
    void rejectsRoleBindingFromAnotherTenant() {
        RbacUserRoleMapper mapper = mock(RbacUserRoleMapper.class);
        RbacRoleBiz roleBiz = mock(RbacRoleBiz.class);
        RbacUserRoleBiz biz = createBiz(mapper, true);
        ReflectionTestUtils.setField(biz, "rbacRoleBiz", roleBiz);
        BaseContextHandler.setUserId("tenant-admin");
        TenantContext.setTenantId("tenant-1");

        RbacRole role = role(202L, "tenant-2");
        when(roleBiz.getRoleForBinding(202L)).thenReturn(role);
        when(roleBiz.isRoleInTenantScope(role, "tenant-1")).thenReturn(false);

        assertThrows(BuzzException.class, () -> biz.save(binding(null, "user-1", 202L)));
        verify(mapper, never()).insert(any(RbacUserRole.class));
    }

    private RbacUserRole binding(Long id, String userId, Long roleId) {
        RbacUserRole binding = new RbacUserRole();
        binding.setId(id);
        binding.setUserId(userId);
        binding.setRoleId(roleId);
        binding.setDeleted(false);
        return binding;
    }

    private RbacRole role(Long id, String tenantId) {
        RbacRole role = new RbacRole();
        role.setId(id);
        role.setType(RbacRoleTypeEnum.TENANT);
        role.setTenantId(tenantId);
        return role;
    }

    private RbacUserRoleBiz createBiz(RbacUserRoleMapper mapper, boolean tenantEnabled) {
        RbacUserRoleBiz biz = new RbacUserRoleBiz();
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);

        FaSetting faSetting = new FaSetting();
        FaSetting.Tenant tenant = new FaSetting.Tenant();
        tenant.setEnabled(tenantEnabled);
        faSetting.setTenant(tenant);
        ReflectionTestUtils.setField(biz, "faSetting", faSetting);
        return biz;
    }
}
