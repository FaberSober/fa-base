package com.faber.api.base.tn.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.rbac.biz.RbacMenuBiz;
import com.faber.api.base.rbac.entity.RbacMenu;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.entity.TenantPermission;
import com.faber.api.base.tn.mapper.TenantPermissionMapper;
import com.faber.api.base.tn.vo.req.TenantPermissionUpdateVo;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户可用权限范围 C_t。
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TenantPermissionBiz extends BaseBiz<TenantPermissionMapper, TenantPermission> {

    @Resource
    private TenantBiz tenantBiz;

    @Resource
    private RbacMenuBiz rbacMenuBiz;

    @Lazy
    @Resource
    private TenantUserBiz tenantUserBiz;

    /**
     * 查询租户可用权限。平台管理员可查询任意租户，租户管理员只能查询当前租户。
     */
    public List<Long> getMenuIds(String tenantId) {
        if (!isTenantEnabled()) {
            return getPlatformMenuIds();
        }
        requireTenant(tenantId);
        checkCanView(tenantId);
        return listTenantMenuIds(tenantId);
    }

    /**
     * 提供给后续角色授权校验使用的租户权限上限。
     */
    public List<Long> getAllowedMenuIds(String tenantId) {
        if (!isTenantEnabled()) {
            return getPlatformMenuIds();
        }
        requireTenant(tenantId);
        return listTenantMenuIds(tenantId);
    }

    /**
     * 创建租户时，初始化表单选择的租户可用权限范围 C_t。
     */
    public void initializePermissions(String tenantId, Collection<Long> menuIds) {
        if (!isTenantEnabled()) {
            return;
        }
        String normalizedTenantId = requireTenantId(tenantId);
        requireTenant(normalizedTenantId);

        Set<Long> requested = normalizeMenuIds(menuIds);
        if (requested.isEmpty()) {
            throw new BuzzException("至少选择一个租户权限点");
        }
        checkMenuIdsInPlatform(requested);
        replaceMenuIds(normalizedTenantId, requested);
    }

    /**
     * 校验一组权限点是否属于租户当前可用范围 C_t。
     */
    public void checkMenuIdsInTenant(String tenantId, Collection<Long> menuIds) {
        if (!isTenantEnabled()) {
            return;
        }
        if (!new HashSet<>(getAllowedMenuIds(tenantId)).containsAll(normalizeMenuIds(menuIds))) {
            throw new BuzzException("角色权限超出当前租户可用权限范围");
        }
    }

    @FaCacheClear(pre = "rbac:")
    public void updateMenuIds(TenantPermissionUpdateVo vo) {
        checkCanUpdate();
        if (vo == null) {
            throw new BuzzException("租户权限参数不能为空");
        }
        String tenantId = requireTenantId(vo.getTenantId());
        requireTenant(tenantId);

        Set<Long> requested = normalizeMenuIds(vo.getMenuIds());
        if (!new HashSet<>(getPlatformMenuIds()).containsAll(requested)) {
            throw new BuzzException("租户权限必须属于平台权限全集");
        }

        replaceMenuIds(tenantId, requested);
    }

    private void replaceMenuIds(String tenantId, Set<Long> requested) {
        List<TenantPermission> current = listTenantPermissions(tenantId);
        Set<Long> currentMenuIds = current.stream()
                .map(TenantPermission::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        current.stream()
                .filter(item -> !requested.contains(item.getMenuId()))
                .forEach(item -> super.removeById(item.getId()));

        for (Long menuId : requested) {
            if (currentMenuIds.contains(menuId)) {
                continue;
            }
            TenantPermission history = baseMapper.selectByTenantIdAndMenuIdIgnoreLogic(tenantId, menuId);
            if (history == null) {
                TenantPermission item = new TenantPermission();
                item.setTenantId(tenantId);
                item.setMenuId(menuId);
                super.save(item);
            } else if (Boolean.TRUE.equals(history.getDeleted())) {
                history.setDeleted(false);
                baseMapper.updateByIdIgnoreLogic(history);
            }
        }
    }

    private void checkMenuIdsInPlatform(Collection<Long> menuIds) {
        if (!new HashSet<>(getPlatformMenuIds()).containsAll(menuIds)) {
            throw new BuzzException("租户权限必须属于平台权限全集");
        }
    }

    private List<TenantPermission> listTenantPermissions(String tenantId) {
        return baseMapper.selectList(new QueryWrapper<TenantPermission>()
                .eq("tenant_id", tenantId));
    }

    private List<Long> listTenantMenuIds(String tenantId) {
        return listTenantPermissions(tenantId).stream()
                .map(TenantPermission::getMenuId)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<Long> getPlatformMenuIds() {
        List<RbacMenu> menus = rbacMenuBiz.list();
        if (menus == null) {
            return Collections.emptyList();
        }
        return menus.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .filter(item -> Boolean.TRUE.equals(item.getStatus()))
                .map(RbacMenu::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Set<Long> normalizeMenuIds(Collection<Long> menuIds) {
        if (menuIds == null) {
            return Collections.emptySet();
        }
        LinkedHashSet<Long> result = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            if (menuId == null) {
                throw new BuzzException("权限ID不能为空");
            }
            result.add(menuId);
        }
        return result;
    }

    private void checkCanUpdate() {
        if (!isTenantEnabled()) {
            throw new BuzzException("租户模式未启用");
        }
        if (!isSuperAdminUser(getCurrentUserId())) {
            throw new BuzzException("仅平台管理员可维护租户权限");
        }
    }

    private void checkCanView(String tenantId) {
        if (isSuperAdminUser(getCurrentUserId())) {
            return;
        }
        if (!StrUtil.equals(tenantId, getCurrentTenantId())
                || !tenantUserBiz.isTenantAdminUser(getCurrentUserId(), tenantId)) {
            throw new BuzzException("无权查看当前租户权限");
        }
    }

    private void requireTenant(String tenantId) {
        String normalizedTenantId = requireTenantId(tenantId);
        Tenant tenant = tenantBiz.getById(normalizedTenantId);
        if (tenant == null) {
            throw new BuzzException("租户不存在");
        }
    }

    private String requireTenantId(String tenantId) {
        String normalizedTenantId = StrUtil.trim(tenantId);
        if (StrUtil.isBlank(normalizedTenantId)) {
            throw new BuzzException("租户ID不能为空");
        }
        return normalizedTenantId;
    }

}
