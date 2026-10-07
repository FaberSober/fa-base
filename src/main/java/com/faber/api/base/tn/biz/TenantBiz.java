package com.faber.api.base.tn.biz;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.rbac.biz.RbacRoleBiz;
import com.faber.api.base.rbac.biz.RbacRoleMenuBiz;
import com.faber.api.base.rbac.biz.RbacUserRoleBiz;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.entity.TenantUser;
import com.faber.api.base.tn.mapper.TenantMapper;
import com.faber.api.base.tn.vo.req.TenantPermissionUpdateVo;
import com.faber.api.base.tn.vo.req.TenantPanelOrderReq;
import com.faber.core.constant.CommonConstants;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TenantBiz extends BaseBiz<TenantMapper, Tenant> {

    @Lazy
    @Resource
    private TenantUserBiz tenantUserBiz;

    @Lazy
    @Resource
    private RbacRoleBiz rbacRoleBiz;

    @Lazy
    @Resource
    private RbacRoleMenuBiz rbacRoleMenuBiz;

    @Lazy
    @Resource
    private RbacUserRoleBiz rbacUserRoleBiz;

    @Lazy
    @Resource
    private TenantPermissionBiz tenantPermissionBiz;

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean save(Tenant entity) {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式请使用带权限的租户创建接口");
        }
        return super.save(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveBatch(Collection<Tenant> entityList) {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式请使用带权限的租户创建接口");
        }
        return super.saveBatch(entityList);
    }

    @Override
    public boolean saveOrUpdate(Tenant entity) {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式请使用带权限的租户创建接口或更新接口");
        }
        return super.saveOrUpdate(entity);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<Tenant> entityList) {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式请使用带权限的租户创建接口或更新接口");
        }
        return super.saveOrUpdateBatch(entityList);
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<Tenant> entityList, int batchSize) {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式请使用带权限的租户创建接口或更新接口");
        }
        return super.saveOrUpdateBatch(entityList, batchSize);
    }

    @FaCacheClear(pre = "rbac:")
    public Tenant createWithPermissions(Tenant entity, Collection<Long> menuIds) {
        if (entity == null) {
            throw new BuzzException("租户参数不能为空");
        }
        if (!super.save(entity)) {
            throw new BuzzException("租户创建失败");
        }
        if (isTenantEnabled()) {
            syncTenantLifecycle(entity, true, menuIds);
        }
        return entity;
    }

    /**
     * 确保系统默认租户存在。该初始化不受多租户开关影响。
     */
    @FaCacheClear(pre = "rbac:")
    public Tenant ensureDefaultTenant() {
        final String defaultTenantCode = "DEFAULT";
        Tenant tenant = baseMapper.selectByCodeIgnoreLogic(defaultTenantCode);
        if (tenant == null) {
            tenant = new Tenant();
            tenant.setCode(defaultTenantCode);
            tenant.setName("默认租户");
            tenant.setShortName("默认租户");
            tenant.setStatus(true);
            tenant.setSort(0);
            tenant.setDescription("系统默认租户");
            if (!super.save(tenant)) {
                throw new BuzzException("系统默认租户创建失败");
            }
            return tenant;
        }

        if (Boolean.TRUE.equals(tenant.getDeleted())) {
            tenant.setDeleted(false);
            tenant.setStatus(true);
            if (baseMapper.updateByIdIgnoreLogic(tenant) != 1) {
                throw new BuzzException("系统默认租户恢复失败");
            }
        }
        return tenant;
    }

    /**
     * 初始化默认租户的权限范围、管理员角色和平台超级管理员关联。
     */
    @FaCacheClear(pre = "rbac:")
    public void initializeDefaultTenantAccess(Tenant tenant) {
        if (!isTenantEnabled() || tenant == null || StrUtil.isBlank(tenant.getId())) {
            return;
        }

        tenantPermissionBiz.initializeAllPermissions(tenant.getId());
        RbacRole tenantAdminRole = rbacRoleBiz.ensureTenantAdminRole(tenant.getId());
        if (tenantAdminRole == null || tenantAdminRole.getId() == null) {
            throw new BuzzException("默认租户管理员角色初始化失败");
        }

        rbacRoleMenuBiz.syncRoleMenus(
                tenantAdminRole.getId(),
                tenantPermissionBiz.getAllowedMenuIds(tenant.getId())
        );
        tenantUserBiz.ensureTenantAdmin(tenant.getId(), CommonConstants.SUPER_ADMIN_ID);
        rbacUserRoleBiz.ensureUserRole(CommonConstants.SUPER_ADMIN_ID, tenantAdminRole.getId());
    }

    @FaCacheClear(pre = "rbac:")
    public Tenant updateWithPermissions(Tenant entity, Collection<Long> menuIds) {
        if (entity == null || StrUtil.isBlank(entity.getId())) {
            throw new BuzzException("租户参数或租户ID不能为空");
        }
        if (!super.updateById(entity)) {
            throw new BuzzException("租户更新失败");
        }
        if (isTenantEnabled()) {
            TenantPermissionUpdateVo vo = new TenantPermissionUpdateVo();
            vo.setTenantId(entity.getId());
            vo.setMenuIds(menuIds == null ? List.of() : new ArrayList<>(menuIds));
            tenantPermissionBiz.updateMenuIds(vo);
        }
        return entity;
    }

    @FaCacheClear(pre = "rbac:")
    public List<TenantUser> savePanelOrder(TenantPanelOrderReq req) {
        String userId = getCurrentUserId();
        if (!isSuperAdminUser(userId)) {
            throw new BuzzException("仅超级管理员可以设置平台租户排序");
        }

        List<TenantUser> tenants = tenantUserBiz.getUserTenants(userId);
        Set<String> availableTenantIds = tenants.stream()
                .map(TenantUser::getTenantId)
                .collect(Collectors.toSet());
        List<String> tenantIds = req.getTenantIds();
        Set<String> submittedTenantIds = new HashSet<>(tenantIds);
        if (submittedTenantIds.size() != tenantIds.size()
                || !submittedTenantIds.equals(availableTenantIds)) {
            throw new BuzzException("租户列表已变化，请刷新后重试");
        }

        Map<String, TenantUser> tenantById = tenants.stream()
                .collect(Collectors.toMap(TenantUser::getTenantId, item -> item));
        for (int i = 0; i < tenantIds.size(); i++) {
            TenantUser tenant = tenantById.get(tenantIds.get(i));
            Tenant update = new Tenant();
            update.setId(tenant.getTenantId());
            update.setSort(i + 1);
            if (baseMapper.updateById(update) != 1) {
                throw new BuzzException("租户列表已变化，请刷新后重试");
            }
        }

        return tenantUserBiz.getUserTenants(userId);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateBatchById(Collection<Tenant> entityList) {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式请使用带权限的租户更新接口");
        }
        return super.updateBatchById(entityList);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateBatchById(Collection<Tenant> entityList, int batchSize) {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式请使用带权限的租户更新接口");
        }
        return super.updateBatchById(entityList, batchSize);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateById(Tenant entity) {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式请使用带权限的租户更新接口");
        }
        boolean updated = super.updateById(entity);
        return updated;
    }

    void syncTenantLifecycle(Tenant entity, boolean created, Collection<Long> menuIds) {
        if (!isTenantEnabled() || entity == null || StrUtil.isBlank(entity.getId())) {
            return;
        }

        if (created) {
            tenantPermissionBiz.initializePermissions(entity.getId(), menuIds);
        }

        RbacRole tenantAdminRole = rbacRoleBiz.ensureTenantAdminRole(entity.getId());
        if (created) {
            if (tenantAdminRole != null && tenantAdminRole.getId() != null) {
                rbacRoleMenuBiz.ensureRoleMenus(
                        tenantAdminRole.getId(),
                        tenantPermissionBiz.getAllowedMenuIds(entity.getId())
                );
            }
            tenantUserBiz.ensureTenantAdmin(entity.getId(), getCurrentUserId());
            if (tenantAdminRole != null && tenantAdminRole.getId() != null) {
                rbacUserRoleBiz.ensureUserRole(getCurrentUserId(), tenantAdminRole.getId());
            }
        }
    }

    /**
     * 同步租户管理员角色权限，调用方必须已完成平台管理员校验。
     */
    public void syncTenantAdminRolePermissions(String tenantId, Collection<Long> menuIds) {
        if (!isTenantEnabled() || StrUtil.isBlank(tenantId)) {
            return;
        }
        RbacRole tenantAdminRole = rbacRoleBiz.ensureTenantAdminRole(tenantId);
        if (tenantAdminRole == null || tenantAdminRole.getId() == null) {
            throw new BuzzException("租户管理员角色不存在");
        }
        rbacRoleMenuBiz.syncRoleMenus(tenantAdminRole.getId(), menuIds);
    }

    @Override
    protected void saveBefore(Tenant entity) {
        entity.setCode(StrUtil.trim(entity.getCode()));
        entity.setName(StrUtil.trim(entity.getName()));
        entity.setShortName(StrUtil.trim(entity.getShortName()));
        entity.setContactName(StrUtil.trim(entity.getContactName()));
        entity.setContactPhone(StrUtil.trim(entity.getContactPhone()));
        entity.setContactEmail(StrUtil.trim(entity.getContactEmail()));
        entity.setDescription(StrUtil.trim(entity.getDescription()));
        if (entity.getSort() == null) {
            entity.setSort(0);
        }

        long count = lambdaQuery()
                .eq(Tenant::getCode, entity.getCode())
                .ne(StrUtil.isNotBlank(entity.getId()), Tenant::getId, entity.getId())
                .count();
        if (count > 0) {
            throw new BuzzException("租户编码重复");
        }
    }

}
