package com.faber.api.base.tn.biz;

import com.faber.api.base.rbac.biz.RbacMenuBiz;
import com.faber.api.base.rbac.entity.RbacMenu;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.entity.TenantPermission;
import com.faber.api.base.tn.mapper.TenantPermissionMapper;
import com.faber.api.base.tn.vo.req.TenantPermissionUpdateVo;
import com.faber.api.base.tn.vo.ret.TenantPermissionScopeVo;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantPermissionBizTest {

    private TenantBiz tenantBiz;

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
        TenantContext.clear();
    }

    @Test
    void platformAdminCanUpdateTenantPermissions() {
        TenantPermissionMapper mapper = mock(TenantPermissionMapper.class);
        RbacMenuBiz menuBiz = mock(RbacMenuBiz.class);
        TenantPermissionBiz biz = createBiz(mapper, menuBiz);
        BaseContextHandler.setUserId("1");

        when(menuBiz.list()).thenReturn(List.of(menu(10L), menu(20L)));
        when(mapper.selectList(any())).thenReturn(List.of());
        when(mapper.selectByTenantIdAndMenuIdIgnoreLogic("tenant-1", 10L)).thenReturn(null);
        when(mapper.selectByTenantIdAndMenuIdIgnoreLogic("tenant-1", 20L)).thenReturn(null);

        TenantPermissionUpdateVo request = new TenantPermissionUpdateVo();
        request.setTenantId("tenant-1");
        request.setMenuIds(List.of(10L, 20L));

        biz.updateMenuIds(request);

        verify(mapper, org.mockito.Mockito.times(2)).insert(any(TenantPermission.class));
        verify(tenantBiz).syncTenantAdminRolePermissions(eq("tenant-1"), any());
    }

    @Test
    void initializesSelectedPlatformPermissionsForNewTenant() {
        TenantPermissionMapper mapper = mock(TenantPermissionMapper.class);
        RbacMenuBiz menuBiz = mock(RbacMenuBiz.class);
        TenantPermissionBiz biz = createBiz(mapper, menuBiz);

        when(menuBiz.list()).thenReturn(List.of(menu(10L), menu(20L)));
        when(mapper.selectList(any())).thenReturn(List.of());
        when(mapper.selectByTenantIdAndMenuIdIgnoreLogic("tenant-1", 10L)).thenReturn(null);
        when(mapper.selectByTenantIdAndMenuIdIgnoreLogic("tenant-1", 20L)).thenReturn(null);

        biz.initializePermissions("tenant-1", List.of(10L));

        verify(mapper).insert(any(TenantPermission.class));
    }

    @Test
    void mergesRequiredPermissionsAndTreatsLegacyRowsAsOptional() {
        TenantPermissionMapper mapper = mock(TenantPermissionMapper.class);
        RbacMenuBiz menuBiz = mock(RbacMenuBiz.class);
        TenantPermissionBiz biz = createBiz(mapper, menuBiz);
        BaseContextHandler.setUserId("1");

        TenantPermission optional = new TenantPermission();
        optional.setTenantId("tenant-1");
        optional.setMenuId(20L);
        when(menuBiz.list()).thenReturn(List.of(menu(10L, true), menu(20L, false)));
        when(mapper.selectList(any())).thenReturn(List.of(optional));

        TenantPermissionScopeVo scope = biz.getPermissionScope("tenant-1");

        assertEquals(List.of(10L), scope.getRequiredMenuIds());
        assertEquals(List.of(20L), scope.getOptionalMenuIds());
        assertEquals(List.of(10L, 20L), scope.getMenuIds());
    }

    @Test
    void doesNotPersistRequiredPermissionWhenInitializingTenant() {
        TenantPermissionMapper mapper = mock(TenantPermissionMapper.class);
        RbacMenuBiz menuBiz = mock(RbacMenuBiz.class);
        TenantPermissionBiz biz = createBiz(mapper, menuBiz);

        when(menuBiz.list()).thenReturn(List.of(menu(10L, true), menu(20L, false)));
        when(mapper.selectList(any())).thenReturn(List.of());
        when(mapper.selectByTenantIdAndMenuIdIgnoreLogic("tenant-1", 20L)).thenReturn(null);

        biz.initializePermissions("tenant-1", List.of(10L, 20L));

        ArgumentCaptor<TenantPermission> captor = ArgumentCaptor.forClass(TenantPermission.class);
        verify(mapper).insert(captor.capture());
        assertEquals(20L, captor.getValue().getMenuId());
    }

    @Test
    void rejectsPermissionOutsidePlatformSet() {
        TenantPermissionMapper mapper = mock(TenantPermissionMapper.class);
        RbacMenuBiz menuBiz = mock(RbacMenuBiz.class);
        TenantPermissionBiz biz = createBiz(mapper, menuBiz);
        BaseContextHandler.setUserId("1");
        when(menuBiz.list()).thenReturn(List.of(menu(10L)));

        TenantPermissionUpdateVo request = new TenantPermissionUpdateVo();
        request.setTenantId("tenant-1");
        request.setMenuIds(List.of(99L));

        assertThrows(BuzzException.class, () -> biz.updateMenuIds(request));
        verify(mapper, never()).insert(any(TenantPermission.class));
    }

    @Test
    void tenantAdminCanReadButCannotUpdateTenantPermissions() {
        TenantPermissionMapper mapper = mock(TenantPermissionMapper.class);
        RbacMenuBiz menuBiz = mock(RbacMenuBiz.class);
        TenantUserBiz tenantUserBiz = mock(TenantUserBiz.class);
        TenantPermissionBiz biz = createBiz(mapper, menuBiz);
        ReflectionTestUtils.setField(biz, "tenantUserBiz", tenantUserBiz);
        BaseContextHandler.setUserId("user-1");
        TenantContext.setTenantId("tenant-1");
        when(tenantUserBiz.isTenantAdminUser("user-1", "tenant-1")).thenReturn(true);

        TenantPermission permission = new TenantPermission();
        permission.setTenantId("tenant-1");
        permission.setMenuId(10L);
        when(menuBiz.list()).thenReturn(List.of(menu(10L)));
        when(mapper.selectList(any())).thenReturn(List.of(permission));

        assertEquals(List.of(10L), biz.getMenuIds("tenant-1"));

        TenantPermissionUpdateVo request = new TenantPermissionUpdateVo();
        request.setTenantId("tenant-1");
        request.setMenuIds(List.of(10L));
        assertThrows(BuzzException.class, () -> biz.updateMenuIds(request));
        verify(mapper, never()).insert(any(TenantPermission.class));
    }

    private TenantPermissionBiz createBiz(TenantPermissionMapper mapper, RbacMenuBiz menuBiz) {
        TenantPermissionBiz biz = new TenantPermissionBiz();
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);
        ReflectionTestUtils.setField(biz, "rbacMenuBiz", menuBiz);

        tenantBiz = mock(TenantBiz.class);
        when(tenantBiz.getById("tenant-1")).thenReturn(new Tenant());
        ReflectionTestUtils.setField(biz, "tenantBiz", tenantBiz);

        FaSetting faSetting = new FaSetting();
        FaSetting.Tenant tenantSetting = new FaSetting.Tenant();
        tenantSetting.setEnabled(true);
        faSetting.setTenant(tenantSetting);
        ReflectionTestUtils.setField(biz, "faSetting", faSetting);
        return biz;
    }

    private RbacMenu menu(Long id) {
        return menu(id, false);
    }

    private RbacMenu menu(Long id, boolean required) {
        RbacMenu menu = new RbacMenu();
        menu.setId(id);
        menu.setDeleted(false);
        menu.setStatus(true);
        menu.setTenantRequired(required);
        return menu;
    }
}
