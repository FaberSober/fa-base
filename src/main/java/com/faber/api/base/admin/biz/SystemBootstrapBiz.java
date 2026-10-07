package com.faber.api.base.admin.biz;

import com.faber.api.base.rbac.biz.RbacRoleMenuBiz;
import com.faber.api.base.tn.biz.TenantBiz;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.core.constant.FaSetting;
import jakarta.annotation.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 系统数据库脚本执行后的首次启动数据初始化。
 */
@Service
public class SystemBootstrapBiz {

    private static final String ADMIN_ROLE_MENU_STATE = "admin_role_menu";
    private static final String DEFAULT_TENANT_ACCESS_STATE = "default_tenant_access";

    @Resource
    private JdbcTemplate jdbcTemplate;

    @Resource
    private FaSetting faSetting;

    @Resource
    private RbacRoleMenuBiz rbacRoleMenuBiz;

    @Resource
    private TenantBiz tenantBiz;

    @Transactional(rollbackFor = Exception.class)
    public void initializeAfterDbScripts() {
        Tenant defaultTenant = tenantBiz.ensureDefaultTenant();

        if (!isComplete(ADMIN_ROLE_MENU_STATE)) {
            rbacRoleMenuBiz.initAdminRoleMenu();
            markComplete(ADMIN_ROLE_MENU_STATE);
        }

        if (faSetting.isTenantEnabled() && !isComplete(DEFAULT_TENANT_ACCESS_STATE)) {
            tenantBiz.initializeDefaultTenantAccess(defaultTenant);
            markComplete(DEFAULT_TENANT_ACCESS_STATE);
        }
    }

    private boolean isComplete(String stateKey) {
        Boolean completed = jdbcTemplate.queryForObject(
                "SELECT completed FROM base_system_bootstrap_state WHERE state_key = ?",
                Boolean.class,
                stateKey
        );
        if (completed == null) {
            throw new IllegalStateException("数据库初始化状态不存在：" + stateKey);
        }
        return completed;
    }

    private void markComplete(String stateKey) {
        int updated = jdbcTemplate.update(
                "UPDATE base_system_bootstrap_state SET completed = ? WHERE state_key = ?",
                true,
                stateKey
        );
        if (updated != 1) {
            throw new IllegalStateException("更新数据库初始化状态失败：" + stateKey);
        }
    }
}
