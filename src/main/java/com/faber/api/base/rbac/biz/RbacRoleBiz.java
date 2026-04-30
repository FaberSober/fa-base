package com.faber.api.base.rbac.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.api.base.rbac.mapper.RbacRoleMapper;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.exception.BuzzException;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

/**
 * BASE-角色表
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-19 11:40:40
 */
@Service
public class RbacRoleBiz extends BaseBiz<RbacRoleMapper, RbacRole> {

    @Lazy
    @Resource
    private TenantUserBiz tenantUserBiz;

    @Override
    public QueryWrapper<RbacRole> parseQuery(QueryParams query) {
        QueryWrapper<RbacRole> wrapper = super.parseQuery(query);
        appendRoleScopeQuery(wrapper);
        return wrapper;
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean save(RbacRole entity) {
        fillAndCheckSaveRole(entity);
        return super.save(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateById(RbacRole entity) {
        fillAndCheckUpdateRole(entity);
        return super.updateById(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean removeById(Serializable id) {
        RbacRole role = getById(id);
        checkCanManageRole(role);
        return super.removeById(id);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void removeBatchByIds(List<Serializable> ids) {
        ids.forEach(id -> checkCanManageRole(getById(id)));
        super.removeBatchByIds(ids);
    }

    public RbacRole getRoleByName(String name) {
        long count = lambdaQuery().eq(RbacRole::getName, name).count();
        if (count != 1) throw new BuzzException("请联系管理检查角色配置：" + name);

        return lambdaQuery().eq(RbacRole::getName, name).one();
    }

    public List<RbacRole> listVisibleRolesByIds(List<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return List.of();
        }
        QueryWrapper<RbacRole> wrapper = new QueryWrapper<>();
        wrapper.eq("status", true).in("id", roleIds);
        appendRoleScopeQuery(wrapper);
        return list(wrapper);
    }

    public void checkCanViewRole(Long roleId) {
        RbacRole role = getById(roleId);
        if (role == null) {
            throw new BuzzException("角色不存在");
        }
        if (canViewRole(role)) {
            return;
        }
        throw new BuzzException("无权查看该角色");
    }

    public void checkCanManageRole(Long roleId) {
        RbacRole role = getById(roleId);
        checkCanManageRole(role);
    }

    public void checkCanManageRole(RbacRole role) {
        if (role == null) {
            throw new BuzzException("角色不存在");
        }
        if (canManageRole(role)) {
            return;
        }
        throw new BuzzException("无权管理该角色");
    }

    private void appendRoleScopeQuery(QueryWrapper<RbacRole> wrapper) {
        if (isSuperAdminUser(getCurrentUserId())) {
            return;
        }

        String tenantId = getCurrentTenantId();
        wrapper.and(ew -> {
            ew.eq("`global`", true).or().isNull("`global`");
            if (StrUtil.isNotBlank(tenantId)) {
                ew.or().eq("tenant_id", tenantId);
            }
        });
    }

    private boolean canViewRole(RbacRole role) {
        if (isSuperAdminUser(getCurrentUserId())) {
            return true;
        }
        if (isGlobalRole(role)) {
            return true;
        }
        String tenantId = getCurrentTenantId();
        return StrUtil.isNotBlank(tenantId) && StrUtil.equals(tenantId, role.getTenantId());
    }

    private boolean canManageRole(RbacRole role) {
        if (isSuperAdminUser(getCurrentUserId())) {
            return true;
        }
        if (isGlobalRole(role)) {
            return false;
        }
        String tenantId = getCurrentTenantId();
        return StrUtil.isNotBlank(tenantId)
                && StrUtil.equals(tenantId, role.getTenantId())
                && tenantUserBiz.isTenantAdminUser(getCurrentUserId(), tenantId);
    }

    private void fillAndCheckSaveRole(RbacRole entity) {
        if (isSuperAdminUser(getCurrentUserId())) {
            fillSuperAdminRoleScope(entity);
            return;
        }

        String tenantId = getCurrentTenantId();
        if (!tenantUserBiz.isTenantAdminUser(getCurrentUserId(), tenantId)) {
            throw new BuzzException("无权新增角色");
        }
        entity.setGlobal(false);
        entity.setTenantId(tenantId);
    }

    private void fillAndCheckUpdateRole(RbacRole entity) {
        RbacRole db = getById(entity.getId());
        checkCanManageRole(db);

        if (isSuperAdminUser(getCurrentUserId())) {
            fillSuperAdminRoleScope(entity);
            return;
        }

        entity.setGlobal(false);
        entity.setTenantId(db.getTenantId());
    }

    private void fillSuperAdminRoleScope(RbacRole entity) {
        if (Boolean.TRUE.equals(entity.getGlobal())) {
            entity.setTenantId(null);
            return;
        }
        entity.setGlobal(false);
        if (StrUtil.isBlank(entity.getTenantId())) {
            entity.setTenantId(getCurrentTenantId());
        }
        if (StrUtil.isBlank(entity.getTenantId())) {
            throw new BuzzException("租户角色需要指定租户ID");
        }
    }

    private boolean isGlobalRole(RbacRole role) {
        return Boolean.TRUE.equals(role.getGlobal()) || (role.getGlobal() == null && StrUtil.isBlank(role.getTenantId()));
    }

}
