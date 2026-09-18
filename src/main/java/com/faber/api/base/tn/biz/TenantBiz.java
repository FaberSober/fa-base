package com.faber.api.base.tn.biz;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.rbac.biz.RbacRoleBiz;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.mapper.TenantMapper;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean save(Tenant entity) {
        boolean saved = super.save(entity);
        if (saved) {
            syncTenantLifecycle(entity, true);
        }
        return saved;
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateById(Tenant entity) {
        boolean updated = super.updateById(entity);
        if (updated) {
            syncTenantLifecycle(entity, false);
        }
        return updated;
    }

    void syncTenantLifecycle(Tenant entity, boolean created) {
        if (!isTenantEnabled() || entity == null || StrUtil.isBlank(entity.getId())) {
            return;
        }

        rbacRoleBiz.ensureTenantAdminRole(entity.getId());
        if (created) {
            tenantUserBiz.ensureTenantAdmin(entity.getId(), getCurrentUserId());
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
