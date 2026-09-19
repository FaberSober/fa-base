package com.faber.api.base.rbac.biz;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.alicp.jetcache.anno.Cached;
import com.faber.api.base.rbac.entity.RbacMenu;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.api.base.rbac.entity.RbacRoleMenu;
import com.faber.api.base.rbac.entity.RbacUserRole;
import com.faber.api.base.rbac.enums.RbacMenuScopeEnum;
import com.faber.api.base.rbac.mapper.RbacUserRoleMapper;
import com.faber.api.base.rbac.vo.RbacUserRoleRetVo;
import com.faber.api.base.rbac.vo.req.RbacUserRoleQueryVo;
import com.faber.api.base.rbac.vo.req.RbacUserRoleUpdateVo;
import com.faber.api.base.rbac.vo.req.RbacUserRolesVo;
import com.faber.api.base.tn.biz.TenantPermissionBiz;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.constant.CommonConstants;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.NoDataException;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import com.faber.core.vo.tree.TreeNode;
import com.faber.core.web.biz.BaseBiz;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BASE-用户角色关联表
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-19 11:40:40
 */
@Service
public class RbacUserRoleBiz extends BaseBiz<RbacUserRoleMapper, RbacUserRole> {

    @Autowired
    private RbacRoleBiz rbacRoleBiz;

    @Autowired
    private RbacRoleMenuBiz rbacRoleMenuBiz;

    @Autowired
    private RbacMenuBiz rbacMenuBiz;

    @Lazy
    @Autowired
    private TenantPermissionBiz tenantPermissionBiz;

    @Lazy
    @Autowired
    private TenantUserBiz tenantUserBiz;

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean save(RbacUserRole entity) {
        checkCanAssignBinding(entity == null ? null : entity.getUserId(), entity == null ? null : entity.getRoleId());
        return super.save(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveBatch(Collection<RbacUserRole> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        entityList.forEach(entity -> checkCanAssignBinding(entity.getUserId(), entity.getRoleId()));
        return super.saveBatch(entityList);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveBatch(Collection<RbacUserRole> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        entityList.forEach(entity -> checkCanAssignBinding(entity.getUserId(), entity.getRoleId()));
        return super.saveBatch(entityList, batchSize);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveOrUpdate(RbacUserRole entity) {
        return entity.getId() == null ? save(entity) : updateById(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveOrUpdateBatch(Collection<RbacUserRole> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        for (RbacUserRole entity : entityList) {
            if (!saveOrUpdate(entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<RbacUserRole> entityList, int batchSize) {
        return saveOrUpdateBatch(entityList);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateById(RbacUserRole entity) {
        RbacUserRole existing = getExisting(entity.getId());
        checkCanManageBinding(existing);
        if (!Objects.equals(existing.getUserId(), entity.getUserId())
                || !Objects.equals(existing.getRoleId(), entity.getRoleId())) {
            throw new BuzzException("用户角色关联的用户和角色不可修改");
        }
        return super.updateById(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateBatchById(Collection<RbacUserRole> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        for (RbacUserRole entity : entityList) {
            if (!updateById(entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updateBatchById(Collection<RbacUserRole> entityList, int batchSize) {
        return updateBatchById(entityList);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean removeById(Serializable id) {
        RbacUserRole userRole = getExisting(id);
        checkCanManageBinding(userRole);
        return super.removeById(id);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void removeBatchByIds(List<Serializable> ids) {
        ids.forEach(id -> checkCanManageBinding(getExisting(id)));
        super.removeBatchByIds(ids);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void removePerById(Serializable id) {
        checkCanManageBinding(getExisting(id));
        super.removePerById(id);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void removePerByIds(Collection<? extends Serializable> ids) {
        ids.forEach(id -> checkCanManageBinding(getExisting(id)));
        super.removePerByIds(ids);
    }

    @Override
    public void removeByQuery(com.faber.core.vo.query.QueryParams query) {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式不支持按条件删除用户角色");
        }
        super.removeByQuery(query);
    }

    @Override
    public void removeMine() {
        if (isTenantEnabled()) {
            throw new BuzzException("多租户模式不支持按条件删除用户角色");
        }
        super.removeMine();
    }

    public List<Long> getUserRoleIds(String userId) {
        List<RbacUserRole> userRoleList = lambdaQuery().eq(RbacUserRole::getUserId, userId).list();
        return userRoleList.stream().map(RbacUserRole::getRoleId).collect(Collectors.toList());
    }

    /**
     * 初始化用户与租户管理员角色的绑定关系。
     */
    @FaCacheClear(pre = "rbac:")
    public void ensureUserRole(String userId, Long roleId) {
        if (StrUtil.isBlank(userId) || roleId == null) {
            return;
        }
        Long count = baseMapper.selectCount(new QueryWrapper<RbacUserRole>()
                .eq("user_id", userId)
                .eq("role_id", roleId));
        if (count != null && count > 0) {
            return;
        }

        RbacUserRole userRole = new RbacUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        super.save(userRole);
    }

    public List<RbacRole> getUserRoles(String userId) {
        List<Long> roleIds = this.getUserRoleIds(userId);
        if (roleIds.isEmpty()) return new ArrayList<>();

        return rbacRoleBiz.listVisibleRolesByIds(roleIds);
    }

    public List<RbacMenu> getUserMenus(String userId, RbacMenuScopeEnum scope) {
        List<Long> roleIds = this.getUserRoles(userId).stream().map(RbacRole::getId).collect(Collectors.toList());
        if (roleIds.isEmpty()) return new ArrayList<>();

        List<RbacRoleMenu> roleMenuList = rbacRoleMenuBiz.lambdaQuery()
                .in(RbacRoleMenu::getRoleId, roleIds)
                .list();
        List<Long> menuIds = roleMenuList.stream().map(RbacRoleMenu::getMenuId).collect(Collectors.toList());
        if (menuIds.isEmpty()) return new ArrayList<>();
        menuIds = limitToTenantPermissions(userId, menuIds);
        if (menuIds.isEmpty()) return new ArrayList<>();

        return rbacMenuBiz.lambdaQuery()
                .eq(RbacMenu::getStatus, true)
                .eq(RbacMenu::getScope, scope)
                .in(RbacMenu::getId, menuIds)
                .orderByAsc(RbacMenu::getSort)
                .list();
    }

//    @Cached(name="rbac:userMenus:", key="#userId")
    public List<TreeNode<RbacMenu>> getUserMenusTree(String userId, RbacMenuScopeEnum scope) {
        List<RbacMenu> list = this.getUserMenus(userId, scope);
        return rbacMenuBiz.listToTree(list, CommonConstants.ROOT);
    }

    private List<Long> limitToTenantPermissions(String userId, List<Long> menuIds) {
        if (!isTenantEnabled() || isSuperAdminUser(userId)) {
            return menuIds;
        }
        String tenantId = getCurrentTenantId();
        if (StrUtil.isBlank(tenantId)) {
            return List.of();
        }
        Set<Long> allowedMenuIds = new HashSet<>(tenantPermissionBiz.getAllowedMenuIds(tenantId));
        return menuIds.stream().filter(allowedMenuIds::contains).distinct().toList();
    }

    /**
     * 校验用户是否有该权限点{@link RbacMenu#getLinkUrl()}
     * @param userId
     * @param linkUrl
     * @return
     */
    public boolean checkUserLinkUrl(String userId, String linkUrl) {
        String tenantId = null;
        if (isTenantEnabled() && !isSuperAdminUser(userId)) {
            tenantId = getCurrentTenantId();
            if (StrUtil.isBlank(tenantId)) {
                return false;
            }
        }
        return baseMapper.countByUserIdAndLinkUrl(userId, linkUrl, tenantId) > 0;
    }

    public TableRet<RbacUserRoleRetVo> pageVo(BasePageQuery<RbacUserRoleQueryVo> query) {
        if (query.getQuery() != null && query.getQuery().getRoleId() != null) {
            rbacRoleBiz.checkCanViewRole(query.getQuery().getRoleId());
        }
        PageInfo<RbacUserRoleRetVo> info = PageHelper.startPage(query.getCurrent(), query.getPageSize())
                .doSelectPageInfo(() -> baseMapper.pageVo(query.getQuery(), query.getSorter()));
        return new TableRet<>(info);
    }

    public void changeUserRoles(String userId, Long roleId) {
        this.changeUserRoles(userId, ListUtil.toList(roleId));
    }

    /**
     * 修改用户角色关联
     * @param userId
     * @param roleIds
     */
    public void changeUserRoles(String userId, List<Long> roleIds) {
        if (StrUtil.isEmpty(userId)) {
            throw new BuzzException("用户ID不能为空");
        }
        if (roleIds == null || roleIds.isEmpty()) {
            throw new BuzzException("更新需要指定角色ID");
        }

        List<RbacUserRole> existingBindings = baseMapper.selectList(new QueryWrapper<RbacUserRole>()
                .eq("user_id", userId));
        Map<Long, RbacRole> existingRoles = loadRoles(existingBindings.stream()
                .map(RbacUserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Set<Long> existingRoleIds = existingRoles.keySet();
        Set<Long> requestedRoleIds = new LinkedHashSet<>();
        for (Long roleId : roleIds) {
            if (roleId == null) {
                throw new BuzzException("角色ID不能为空");
            }
            requestedRoleIds.add(roleId);
        }

        Map<Long, RbacRole> requestedRoles = loadRoles(requestedRoleIds);
        for (Long roleId : requestedRoleIds) {
            RbacRole role = requestedRoles.get(roleId);
            checkRoleBindingScope(role);
            checkUserTenant(userId, role);
            if (!isPreservedRole(existingRoleIds, role)) {
                rbacRoleBiz.checkCanAssignRole(role);
            }
        }

        for (RbacUserRole binding : existingBindings) {
            RbacRole role = existingRoles.get(binding.getRoleId());
            if (isManagedBinding(role) && !requestedRoleIds.contains(binding.getRoleId())) {
                super.removeById(binding.getId());
            }
        }

        for (Long roleId : requestedRoleIds) {
            if (existingRoleIds.contains(roleId)) {
                continue;
            }
            RbacUserRole userRole = new RbacUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            super.save(userRole);
        }
    }

    public void addUsers(RbacUserRoleUpdateVo param) {
        if (param == null || param.getRoleId() == null || param.getUserIds() == null || param.getUserIds().isEmpty()) {
            throw new BuzzException("用户和角色不能为空");
        }
        Long roleId = param.getRoleId();
        RbacRole role = rbacRoleBiz.getRoleForBinding(roleId);
        checkRoleBindingScope(role);
        rbacRoleBiz.checkCanAssignRole(role);
        for (String userId : param.getUserIds()) {
            checkUserTenant(userId, role);
            long count = lambdaQuery()
                    .eq(RbacUserRole::getUserId, userId)
                    .eq(RbacUserRole::getRoleId, roleId)
                    .count();
            if (count == 1) continue;

            if (count > 0) {
                lambdaUpdate()
                        .eq(RbacUserRole::getUserId, userId)
                        .eq(RbacUserRole::getRoleId, roleId)
                        .remove();
            }

            RbacUserRole userRole = new RbacUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            super.save(userRole);
        }
    }

    @Transactional
    @FaCacheClear(pre = "rbac:")
    public void updateUserRoles(RbacUserRolesVo params) {
        if (params == null) {
            throw new BuzzException("用户角色参数不能为空");
        }
        changeUserRoles(params.getUserId(), params.getRoleIds());
    }

    private void checkCanAssignBinding(String userId, Long roleId) {
        if (StrUtil.isBlank(userId) || roleId == null) {
            throw new BuzzException("用户和角色不能为空");
        }
        RbacRole role = rbacRoleBiz.getRoleForBinding(roleId);
        checkRoleBindingScope(role);
        rbacRoleBiz.checkCanAssignRole(role);
        checkUserTenant(userId, role);
    }

    private void checkCanManageBinding(RbacUserRole userRole) {
        if (userRole == null) {
            throw new NoDataException();
        }
        if (Objects.equals(userRole.getRoleId(), 1L)) {
            throw new BuzzException("不能删除默认的超级管理员角色");
        }
        RbacRole role = rbacRoleBiz.getRoleForBinding(userRole.getRoleId());
        checkRoleBindingScope(role);
        rbacRoleBiz.checkCanManageRole(role);
    }

    private void checkRoleBindingScope(RbacRole role) {
        if (isTenantEnabled() && !rbacRoleBiz.isRoleInTenantScope(role, getCurrentTenantId())) {
            throw new BuzzException("角色不属于当前租户范围");
        }
    }

    private void checkUserTenant(String userId, RbacRole role) {
        String tenantId = getCurrentTenantId();
        if (isTenantEnabled() && rbacRoleBiz.isTenantRole(role) && StrUtil.isNotBlank(tenantId)
                && !tenantUserBiz.hasUserTenant(userId, tenantId)) {
            throw new BuzzException("用户未加入当前租户");
        }
    }

    private boolean isPreservedRole(Set<Long> existingRoleIds, RbacRole role) {
        return !isSuperAdminUser(getCurrentUserId())
                && existingRoleIds.contains(role.getId())
                && (!rbacRoleBiz.isTenantRole(role) || rbacRoleBiz.isTenantAdminRole(role));
    }

    private boolean isManagedBinding(RbacRole role) {
        if (!isTenantEnabled()) {
            return true;
        }
        return rbacRoleBiz.isRoleInTenantScope(role, getCurrentTenantId())
                && (isSuperAdminUser(getCurrentUserId())
                || (rbacRoleBiz.isTenantRole(role) && !rbacRoleBiz.isTenantAdminRole(role)));
    }

    private Map<Long, RbacRole> loadRoles(Collection<Long> roleIds) {
        return roleIds.stream()
                .map(rbacRoleBiz::getRoleForBinding)
                .collect(Collectors.toMap(RbacRole::getId, role -> role));
    }

    private RbacUserRole getExisting(Serializable id) {
        RbacUserRole userRole = baseMapper.selectByIdIgnoreLogic(id);
        if (userRole == null || Boolean.TRUE.equals(userRole.getDeleted())) {
            throw new NoDataException();
        }
        return userRole;
    }

}
