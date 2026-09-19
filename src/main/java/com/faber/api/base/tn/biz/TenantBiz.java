package com.faber.api.base.tn.biz;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.rbac.biz.RbacRoleBiz;
import com.faber.api.base.rbac.biz.RbacRoleMenuBiz;
import com.faber.api.base.rbac.biz.RbacUserRoleBiz;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.mapper.TenantMapper;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

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

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateById(Tenant entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            syncTenantLifecycle(entity, false, null);
        }
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
