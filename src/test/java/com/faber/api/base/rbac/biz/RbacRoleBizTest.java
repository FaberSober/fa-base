package com.faber.api.base.rbac.biz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
