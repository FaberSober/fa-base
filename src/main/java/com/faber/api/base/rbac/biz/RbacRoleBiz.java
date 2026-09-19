package com.faber.api.base.rbac.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.rbac.mapper.RbacRoleMapper;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.api.base.rbac.enums.RbacRoleTypeEnum;
import com.faber.api.base.tn.biz.TenantBiz;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.exception.BuzzException;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * BASE-角色表
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-19 11:40:40
 */
@Service
public class RbacRoleBiz extends BaseBiz<RbacRoleMapper, RbacRole> {

    private static final String TENANT_ADMIN_ROLE_NAME = "租户管理员";

    @Lazy
    @Resource
    private TenantUserBiz tenantUserBiz;

    @Lazy
    @Resource
    private TenantBiz tenantBiz;

    @Override
    public QueryWrapper<RbacRole> parseQuery(QueryParams query) {
        QueryWrapper<RbacRole> wrapper = super.parseQuery(query);
        appendRoleScopeQuery(wrapper);
        return wrapper;
    }

    @Override
    public RbacRole getById(Serializable id) {
        RbacRole role = super.getById(id);
        if (role != null) {
            checkCanViewRole(role);
        }
        return role;
    }

    @Override
    public RbacRole getDetailById(Serializable id) {
        RbacRole role = getById(id);
        decorateOne(role);
        return role;
    }

    @Override
    public <ID extends Serializable> List<RbacRole> getByIds(List<ID> ids) {
        List<RbacRole> roles = super.getByIds(ids);
        roles.forEach(this::checkCanViewRole);
        return roles;
    }

    @Override
    public List<RbacRole> list() {
        return list(new QueryParams());
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveBatch(Collection<RbacRole> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        entityList.forEach(this::fillAndCheckSaveRole);
        return super.saveBatch(entityList);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveBatch(Collection<RbacRole> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        entityList.forEach(this::fillAndCheckSaveRole);
        return super.saveBatch(entityList, batchSize);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveOrUpdate(RbacRole entity) {
        return entity.getId() == null ? save(entity) : updateById(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveOrUpdateBatch(Collection<RbacRole> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        for (RbacRole entity : entityList) {
            if (!saveOrUpdate(entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<RbacRole> entityList, int batchSize) {
        return saveOrUpdateBatch(entityList);
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

    @FaCacheClear(pre = "rbac:")
    @Override
    public void removePerById(Serializable id) {
        checkCanManageRole(toRoleId(id));
        super.removePerById(id);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void removePerByIds(Collection<? extends Serializable> ids) {
        ids.forEach(id -> checkCanManageRole(toRoleId(id)));
        super.removePerByIds(ids);
    }

    @Override
    public void removeByQuery(QueryParams query) {
        if (isTenantEnabled()
                && !isSuperAdminUser(getCurrentUserId())
                && !tenantUserBiz.isTenantAdminUser(getCurrentUserId(), getCurrentTenantId())) {
            throw new BuzzException("无权管理角色");
        }
        super.removeByQuery(query);
    }

    public RbacRole getRoleByName(String name) {
        long count = lambdaQuery().eq(RbacRole::getName, name).count();
        if (count != 1) throw new BuzzException("请联系管理检查角色配置：" + name);

        return lambdaQuery().eq(RbacRole::getName, name).one();
    }

    /**
     * 确保租户拥有默认的租户管理员角色。
     */
    public RbacRole ensureTenantAdminRole(String tenantId) {
        if (StrUtil.isBlank(tenantId)) {
            return null;
        }

        QueryWrapper<RbacRole> wrapper = new QueryWrapper<>();
        wrapper.eq("name", TENANT_ADMIN_ROLE_NAME)
                .eq("type", RbacRoleTypeEnum.TENANT.getValue())
                .eq("tenant_id", tenantId);
        RbacRole role = baseMapper.selectOne(wrapper);
        if (role != null) {
            return role;
        }

        role = new RbacRole();
        role.setName(TENANT_ADMIN_ROLE_NAME);
        role.setRemarks("租户默认管理员角色");
        role.setStatus(true);
        role.setType(RbacRoleTypeEnum.TENANT);
        role.setTenantId(tenantId);
        if (!super.save(role) || role.getId() == null) {
            throw new BuzzException("租户管理员角色创建失败");
        }
        return role;
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

    @Override
    public void decorateOne(RbacRole role) {
        if (role == null || StrUtil.isBlank(role.getTenantId())) {
            return;
        }
        Tenant tenant = tenantBiz.getById(role.getTenantId());
        if (tenant != null) {
            role.setTenantName(tenant.getName());
        }
    }

    @Override
    public void decorateList(List<RbacRole> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<String> tenantIds = list.stream()
                .map(RbacRole::getTenantId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        if (tenantIds.isEmpty()) {
            return;
        }
        Map<String, String> tenantNameMap = tenantBiz.listByIds(tenantIds).stream()
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (a, b) -> a));
        list.forEach(role -> role.setTenantName(tenantNameMap.get(role.getTenantId())));
    }

    public void checkCanViewRole(Long roleId) {
        RbacRole role = super.getById(roleId);
        checkCanViewRole(role);
    }

    public void checkCanViewRole(RbacRole role) {
        if (role == null) {
            throw new BuzzException("角色不存在");
        }
        if (canViewRole(role)) {
            return;
        }
        throw new BuzzException("无权查看该角色");
    }

    public void checkCanManageRole(Long roleId) {
        RbacRole role = super.getById(roleId);
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
        if (!isTenantEnabled() || isSuperAdminUser(getCurrentUserId())) {
            return;
        }

        String tenantId = getCurrentTenantId();
        wrapper.and(ew -> {
            ew.eq("type", RbacRoleTypeEnum.GLOBAL.getValue());
            if (StrUtil.isNotBlank(tenantId)) {
                ew.or(query -> query.eq("type", RbacRoleTypeEnum.TENANT.getValue()).eq("tenant_id", tenantId));
            }
        });
    }

    private boolean canViewRole(RbacRole role) {
        if (!isTenantEnabled() || isSuperAdminUser(getCurrentUserId())) {
            return true;
        }
        RbacRoleTypeEnum type = getRoleType(role);
        if (type == RbacRoleTypeEnum.GLOBAL_SUPER) {
            return false;
        }
        if (type == RbacRoleTypeEnum.GLOBAL) {
            return true;
        }
        String tenantId = getCurrentTenantId();
        return StrUtil.isNotBlank(tenantId) && StrUtil.equals(tenantId, role.getTenantId());
    }

    private boolean canManageRole(RbacRole role) {
        if (!isTenantEnabled() || isSuperAdminUser(getCurrentUserId())) {
            return true;
        }
        RbacRoleTypeEnum type = getRoleType(role);
        if (type == RbacRoleTypeEnum.GLOBAL_SUPER) {
            return false;
        }
        String tenantId = getCurrentTenantId();
        if (StrUtil.isBlank(tenantId) || !tenantUserBiz.isTenantAdminUser(getCurrentUserId(), tenantId)) {
            return false;
        }
        if (type == RbacRoleTypeEnum.GLOBAL) {
            return true;
        }
        return StrUtil.equals(tenantId, role.getTenantId());
    }

    private void fillAndCheckSaveRole(RbacRole entity) {
        if (!isTenantEnabled()) {
            return;
        }
        if (isSuperAdminUser(getCurrentUserId())) {
            fillSuperAdminRoleScope(entity);
            return;
        }

        String tenantId = getCurrentTenantId();
        if (!tenantUserBiz.isTenantAdminUser(getCurrentUserId(), tenantId)) {
            throw new BuzzException("无权新增角色");
        }
        fillTenantAdminRoleScope(entity, tenantId);
    }

    private void fillAndCheckUpdateRole(RbacRole entity) {
        if (!isTenantEnabled()) {
            return;
        }
        RbacRole db = super.getById(entity.getId());
        checkCanManageRole(db);

        if (isSuperAdminUser(getCurrentUserId())) {
            fillSuperAdminRoleScope(entity);
            return;
        }

        fillTenantAdminRoleScope(entity, getCurrentTenantId());
    }

    private void fillSuperAdminRoleScope(RbacRole entity) {
        RbacRoleTypeEnum type = entity.getType();
        if (type == null) {
            type = RbacRoleTypeEnum.TENANT;
            entity.setType(type);
        }
        if (type == RbacRoleTypeEnum.GLOBAL_SUPER || type == RbacRoleTypeEnum.GLOBAL) {
            entity.setTenantId(null);
            return;
        }
        if (type != RbacRoleTypeEnum.TENANT) {
            throw new BuzzException("角色类型错误");
        }
        if (StrUtil.isBlank(entity.getTenantId())) {
            entity.setTenantId(getCurrentTenantId());
        }
        if (StrUtil.isBlank(entity.getTenantId())) {
            throw new BuzzException("租户角色需要指定租户ID");
        }
    }

    private void fillTenantAdminRoleScope(RbacRole entity, String tenantId) {
        if (StrUtil.isBlank(tenantId)) {
            throw new BuzzException("当前租户不能为空");
        }
        RbacRoleTypeEnum type = entity.getType() == null ? RbacRoleTypeEnum.TENANT : entity.getType();
        if (type == RbacRoleTypeEnum.GLOBAL_SUPER) {
            throw new BuzzException("无权管理全局超管角色");
        }
        if (type == RbacRoleTypeEnum.GLOBAL) {
            entity.setType(RbacRoleTypeEnum.GLOBAL);
            entity.setTenantId(null);
            return;
        }
        if (type != RbacRoleTypeEnum.TENANT) {
            throw new BuzzException("角色类型错误");
        }
        entity.setType(RbacRoleTypeEnum.TENANT);
        entity.setTenantId(tenantId);
    }

    private RbacRoleTypeEnum getRoleType(RbacRole role) {
        if (role.getType() != null) {
            return role.getType();
        }
        if (role.getId() != null && role.getId() == 1L) {
            return RbacRoleTypeEnum.GLOBAL_SUPER;
        }
        return StrUtil.isBlank(role.getTenantId()) ? RbacRoleTypeEnum.GLOBAL : RbacRoleTypeEnum.TENANT;
    }

    private Long toRoleId(Serializable id) {
        return id instanceof Number ? ((Number) id).longValue() : Long.valueOf(id.toString());
    }

}
