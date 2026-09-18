package com.faber.api.base.rbac.biz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.api.base.rbac.enums.RbacRoleTypeEnum;
import com.faber.api.base.rbac.mapper.RbacRoleMapper;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
