package com.faber.api.base.tn.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.rbac.biz.RbacMenuBiz;
import com.faber.api.base.rbac.entity.RbacMenu;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.entity.TenantPermission;
import com.faber.api.base.tn.mapper.TenantPermissionMapper;
import com.faber.api.base.tn.vo.req.TenantPermissionUpdateVo;
import com.faber.api.base.tn.vo.ret.TenantPermissionScopeVo;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 租户可用权限范围：M、O_t 和 C_t。
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
        return getPermissionScope(tenantId).getMenuIds();
    }

    /**
     * 返回平台必选、租户可选和最终有效权限，供租户管理页面展示。
     */
    public TenantPermissionScopeVo getPermissionScope(String tenantId) {
        if (!isTenantEnabled()) {
            return buildPermissionScope(null);
        }
        requireTenant(tenantId);
        checkCanView(tenantId);
        return buildPermissionScope(tenantId);
    }

    /**
     * 提供给后续角色授权校验使用的租户权限上限。
     */
    public List<Long> getAllowedMenuIds(String tenantId) {
        if (!isTenantEnabled()) {
            return getPlatformMenuIds();
        }
        requireTenant(tenantId);
        return buildPermissionScope(tenantId).getMenuIds();
    }

    /**
     * 创建租户时，初始化表单选择的租户可选权限范围 O_t。
     */
    public void initializePermissions(String tenantId, Collection<Long> menuIds) {
        if (!isTenantEnabled()) {
            return;
        }
        String normalizedTenantId = requireTenantId(tenantId);
        requireTenant(normalizedTenantId);

        Set<Long> requested = normalizeOptionalMenuIds(menuIds);
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
        requested = normalizeOptionalMenuIds(requested);

        replaceMenuIds(tenantId, requested);
        tenantBiz.syncTenantAdminRolePermissions(tenantId, getAllowedMenuIds(tenantId));
    }

    /**
     * 平台必选权限发生变化时，保持租户管理员角色与新的 C_t 一致。
     * 取消 M 时回填 O_t，避免历史租户被静默收权；普通角色不做同步。
     */
    public void syncRequiredPermissionChange(Long menuId, boolean oldRequired, boolean newRequired) {
        if (!isTenantEnabled() || menuId == null || oldRequired == newRequired) {
            return;
        }
        List<Tenant> tenants = tenantBiz.list();
        if (tenants == null) {
            return;
        }
        Date now = new Date();
        for (Tenant tenant : tenants) {
            if (tenant == null || !Boolean.TRUE.equals(tenant.getStatus())
                    || Boolean.TRUE.equals(tenant.getDeleted())
                    || (tenant.getExpireTime() != null && tenant.getExpireTime().before(now))) {
                continue;
            }
            if (newRequired) {
                removeOptionalPermission(tenant.getId(), menuId);
            } else {
                ensureOptionalPermission(tenant.getId(), menuId);
            }
            tenantBiz.syncTenantAdminRolePermissions(tenant.getId(), getAllowedMenuIds(tenant.getId()));
        }
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

    private Set<Long> normalizeOptionalMenuIds(Collection<Long> menuIds) {
        Set<Long> normalized = new LinkedHashSet<>(normalizeMenuIds(menuIds));
        checkMenuIdsInPlatform(normalized);
        normalized.removeAll(new HashSet<>(getRequiredMenuIds()));
        return normalized;
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

    private TenantPermissionScopeVo buildPermissionScope(String tenantId) {
        Set<Long> platformMenuIds = new LinkedHashSet<>(getPlatformMenuIds());
        Set<Long> requiredMenuIds = getRequiredMenuIds().stream()
                .filter(platformMenuIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<Long> optionalMenuIds = new LinkedHashSet<>();
        if (tenantId == null) {
            platformMenuIds.stream()
                    .filter(menuId -> !requiredMenuIds.contains(menuId))
                    .forEach(optionalMenuIds::add);
        } else {
            listTenantMenuIds(tenantId).stream()
                    .filter(platformMenuIds::contains)
                    .filter(menuId -> !requiredMenuIds.contains(menuId))
                    .forEach(optionalMenuIds::add);
        }

        Set<Long> effectiveMenuIds = new LinkedHashSet<>(requiredMenuIds);
        effectiveMenuIds.addAll(optionalMenuIds);
        TenantPermissionScopeVo result = new TenantPermissionScopeVo();
        result.setRequiredMenuIds(new ArrayList<>(requiredMenuIds));
        result.setOptionalMenuIds(new ArrayList<>(optionalMenuIds));
        result.setMenuIds(new ArrayList<>(effectiveMenuIds));
        return result;
    }

    private List<Long> getRequiredMenuIds() {
        return getPlatformMenus().stream()
                .filter(menu -> Boolean.TRUE.equals(menu.getTenantRequired()))
                .map(RbacMenu::getId)
                .toList();
    }

    private List<Long> getPlatformMenuIds() {
        return getPlatformMenus().stream()
                .map(RbacMenu::getId)
                .toList();
    }

    private List<RbacMenu> getPlatformMenus() {
        List<RbacMenu> menus = rbacMenuBiz.list();
        if (menus == null) {
            return Collections.emptyList();
        }
        return menus.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Boolean.TRUE.equals(item.getDeleted()))
                .filter(item -> Boolean.TRUE.equals(item.getStatus()))
                .filter(item -> item.getId() != null)
                .toList();
    }

    private void ensureOptionalPermission(String tenantId, Long menuId) {
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

    private void removeOptionalPermission(String tenantId, Long menuId) {
        listTenantPermissions(tenantId).stream()
                .filter(item -> Objects.equals(item.getMenuId(), menuId))
                .forEach(item -> super.removeById(item.getId()));
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
