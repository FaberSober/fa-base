package com.faber.api.base.rbac.biz;

import com.faber.api.base.rbac.mapper.RbacUserRoleMapper;
import com.faber.api.base.rbac.entity.RbacUserRole;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
