package com.faber.api.base.rbac.biz;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.rbac.entity.RbacRoleMenu;
import com.faber.api.base.rbac.mapper.RbacRoleMenuMapper;
import com.faber.api.base.rbac.vo.RoleMenuVo;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.NoDataException;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * BASE-角色权限对应表
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-19 11:40:40
 */
@Service
public class RbacRoleMenuBiz extends BaseBiz<RbacRoleMenuMapper, RbacRoleMenu> {

    @Lazy
    @Resource
    RbacMenuBiz rbacMenuBiz;

    @Lazy
    @Resource
    RbacRoleBiz rbacRoleBiz;

    @Override
    public QueryWrapper<RbacRoleMenu> parseQuery(QueryParams query) {
        QueryWrapper<RbacRoleMenu> wrapper = super.parseQuery(query);
        if (isTenantEnabled() && !isSuperAdminUser(getCurrentUserId())) {
            rbacRoleBiz.checkCanViewRole(requireRoleId(query));
        }
        return wrapper;
    }

    @Override
    public RbacRoleMenu getById(Serializable id) {
        RbacRoleMenu roleMenu = super.getById(id);
        if (roleMenu != null) {
            rbacRoleBiz.checkCanViewRole(roleMenu.getRoleId());
        }
        return roleMenu;
    }

    @Override
    public RbacRoleMenu getDetailById(Serializable id) {
        RbacRoleMenu roleMenu = getById(id);
        decorateOne(roleMenu);
        return roleMenu;
    }

    @Override
    public <ID extends Serializable> List<RbacRoleMenu> getByIds(List<ID> ids) {
        List<RbacRoleMenu> list = super.getByIds(ids);
        list.forEach(item -> rbacRoleBiz.checkCanViewRole(item.getRoleId()));
        return list;
    }

    @Override
    public List<RbacRoleMenu> list() {
        return list(new QueryParams());
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean save(RbacRoleMenu entity) {
        rbacRoleBiz.checkCanManageRole(requireRoleId(entity));
        return super.save(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveBatch(Collection<RbacRoleMenu> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        entityList.forEach(entity -> rbacRoleBiz.checkCanManageRole(requireRoleId(entity)));
        return super.saveBatch(entityList);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveBatch(Collection<RbacRoleMenu> entityList, int batchSize) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        entityList.forEach(entity -> rbacRoleBiz.checkCanManageRole(requireRoleId(entity)));
        return super.saveBatch(entityList, batchSize);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveOrUpdate(RbacRoleMenu entity) {
        return entity.getId() == null ? save(entity) : updateById(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean saveOrUpdateBatch(Collection<RbacRoleMenu> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        for (RbacRoleMenu entity : entityList) {
            if (!saveOrUpdate(entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<RbacRoleMenu> entityList, int batchSize) {
        return saveOrUpdateBatch(entityList);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateById(RbacRoleMenu entity) {
        RbacRoleMenu existing = getExisting(entity.getId());
        rbacRoleBiz.checkCanManageRole(existing.getRoleId());
        if (!Objects.equals(existing.getRoleId(), entity.getRoleId())) {
            throw new BuzzException("角色权限的角色不可修改");
        }
        return super.updateById(entity);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean updateBatchById(Collection<RbacRoleMenu> entityList) {
        if (entityList == null || entityList.isEmpty()) {
            return true;
        }
        for (RbacRoleMenu entity : entityList) {
            if (!updateById(entity)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean updateBatchById(Collection<RbacRoleMenu> entityList, int batchSize) {
        return updateBatchById(entityList);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean removeById(Serializable id) {
        RbacRoleMenu existing = baseMapper.selectByIdIgnoreLogic(id);
        if (existing != null) {
            rbacRoleBiz.checkCanManageRole(existing.getRoleId());
        }
        return super.removeById(id);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void removeBatchByIds(List<Serializable> ids) {
        ids.forEach(id -> {
            RbacRoleMenu existing = baseMapper.selectByIdIgnoreLogic(id);
            if (existing != null) {
                rbacRoleBiz.checkCanManageRole(existing.getRoleId());
            }
        });
        super.removeBatchByIds(ids);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void removePerById(Serializable id) {
        rbacRoleBiz.checkCanManageRole(getExisting(id).getRoleId());
        super.removePerById(id);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void removePerByIds(Collection<? extends Serializable> ids) {
        ids.forEach(id -> rbacRoleBiz.checkCanManageRole(getExisting(id).getRoleId()));
        super.removePerByIds(ids);
    }

    @Override
    public void removeByQuery(QueryParams query) {
        if (isTenantEnabled() && !isSuperAdminUser(getCurrentUserId())) {
            rbacRoleBiz.checkCanManageRole(requireRoleId(query));
        }
        super.removeByQuery(query);
    }

    public List<Long> getMenuIdsWithHalfCheck(Long roleId) {
        return lambdaQuery()
                .eq(RbacRoleMenu::getRoleId, roleId)
                .list()
                .stream()
                .map(RbacRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    public RoleMenuVo getRoleMenu(Long roleId) {
        rbacRoleBiz.checkCanViewRole(roleId);
        RoleMenuVo vo = new RoleMenuVo();
        vo.setRoleId(roleId);
        vo.setCheckedMenuIds(this.getMenuIdsWithHalfCheck(roleId));
        return vo;
    }

    @FaCacheClear(pre = "rbac:")
    @Transactional
    public void updateRoleMenu(RoleMenuVo roleMenuVo) {
        long roleId = roleMenuVo.getRoleId();
        rbacRoleBiz.checkCanManageRole(roleId);

        if (roleMenuVo.getCheckedMenuIds() == null || roleMenuVo.getCheckedMenuIds().isEmpty()) {
            lambdaUpdate()
                .eq(RbacRoleMenu::getRoleId, roleId)
                .remove();
            return;
        }

        // 删除被移除的角色菜单
        lambdaUpdate()
            .eq(RbacRoleMenu::getRoleId, roleId)
            .notIn(RbacRoleMenu::getMenuId, roleMenuVo.getCheckedMenuIds())
            .remove();

        // 查询已存在的角色菜单
        List<Long> existMenuIds = lambdaQuery()
            .eq(RbacRoleMenu::getRoleId, roleId)
            .list()
            .stream()
            .map(RbacRoleMenu::getMenuId)
            .collect(Collectors.toList());
        // 过滤本次新增的菜单IDs
        roleMenuVo.getCheckedMenuIds().removeAll(existMenuIds);

        List<RbacRoleMenu> list = new ArrayList<>();
        for (Long menuId : roleMenuVo.getCheckedMenuIds()) {
            list.add(new RbacRoleMenu(null, roleId, menuId, false));
        }
//        for (Long menuId : roleMenuVo.getHalfCheckedMenuIds()) {
//            list.add(new RbacRoleMenu(null, roleId, menuId, true));
//        }

        this.saveBatch(list);
    }

    /**
     * 系统第一次启动时，初始化"超级管理员"角色的权限（赋全部权限）
     */
    public void initAdminRoleMenu() {
        // 如果角色权限表已经有数据，则不做初始化
        if (this.count() > 0) return;

        List<RbacRoleMenu> roleMenuList = rbacMenuBiz.list().stream().map(i -> {
            return new RbacRoleMenu(null, 1L, i.getId(), false);
        }).collect(Collectors.toList());

        super.saveBatch(roleMenuList);
    }

    private RbacRoleMenu getExisting(Serializable id) {
        RbacRoleMenu roleMenu = baseMapper.selectByIdIgnoreLogic(id);
        if (roleMenu == null || Boolean.TRUE.equals(roleMenu.getDeleted())) {
            throw new NoDataException();
        }
        return roleMenu;
    }

    private Long requireRoleId(RbacRoleMenu roleMenu) {
        if (roleMenu == null || roleMenu.getRoleId() == null) {
            throw new BuzzException("角色ID不能为空");
        }
        return roleMenu.getRoleId();
    }

    private Long requireRoleId(QueryParams query) {
        Object roleId = query == null || query.getQuery() == null
                ? null : query.getQuery().get("roleId");
        if (roleId == null || roleId.toString().isBlank()) {
            throw new BuzzException("租户模式下必须指定角色ID");
        }
        try {
            return Long.valueOf(roleId.toString());
        } catch (NumberFormatException e) {
            throw new BuzzException("角色ID格式错误");
        }
    }

}
