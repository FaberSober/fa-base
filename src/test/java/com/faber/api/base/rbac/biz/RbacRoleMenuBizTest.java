package com.faber.api.base.rbac.biz;

import com.faber.api.base.rbac.entity.RbacRoleMenu;
import com.faber.api.base.rbac.mapper.RbacRoleMenuMapper;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RbacRoleMenuBizTest {

    @Test
    void saveRejectsRoleWithoutManagementPermission() {
        RbacRoleMenuMapper mapper = mock(RbacRoleMenuMapper.class);
        RbacRoleBiz roleBiz = mock(RbacRoleBiz.class);
        RbacRoleMenuBiz biz = createBiz(mapper, roleBiz);
        doThrow(new BuzzException("无权管理该角色")).when(roleBiz).checkCanManageRole(2L);

        RbacRoleMenu roleMenu = new RbacRoleMenu();
        roleMenu.setRoleId(2L);
        roleMenu.setMenuId(10L);

        assertThrows(BuzzException.class, () -> biz.save(roleMenu));
        verify(mapper, never()).insert(any(RbacRoleMenu.class));
    }

    @Test
    void updateCannotMovePermissionToAnotherRole() {
        RbacRoleMenuMapper mapper = mock(RbacRoleMenuMapper.class);
        RbacRoleBiz roleBiz = mock(RbacRoleBiz.class);
        RbacRoleMenuBiz biz = createBiz(mapper, roleBiz);
        RbacRoleMenu existing = new RbacRoleMenu();
        existing.setId(1L);
        existing.setRoleId(2L);
        existing.setDeleted(false);
        when(mapper.selectByIdIgnoreLogic(1L)).thenReturn(existing);

        RbacRoleMenu request = new RbacRoleMenu();
        request.setId(1L);
        request.setRoleId(3L);
        request.setMenuId(10L);

        assertThrows(BuzzException.class, () -> biz.updateById(request));
        verify(mapper, never()).updateById(any(RbacRoleMenu.class));
    }

    @Test
    void initializesOnlyMissingRolePermissions() {
        RbacRoleMenuMapper mapper = mock(RbacRoleMenuMapper.class);
        RbacRoleBiz roleBiz = mock(RbacRoleBiz.class);
        RbacRoleMenuBiz biz = createBiz(mapper, roleBiz);
        RbacRoleMenu existing = new RbacRoleMenu();
        existing.setRoleId(2L);
        existing.setMenuId(10L);
        when(mapper.selectList(any())).thenReturn(List.of(existing));

        biz.ensureRoleMenus(2L, List.of(10L, 20L));

        ArgumentCaptor<RbacRoleMenu> captor = ArgumentCaptor.forClass(RbacRoleMenu.class);
        verify(mapper).insert(captor.capture());
        assertEquals(20L, captor.getValue().getMenuId());
    }

    private RbacRoleMenuBiz createBiz(RbacRoleMenuMapper mapper, RbacRoleBiz roleBiz) {
        RbacRoleMenuBiz biz = new RbacRoleMenuBiz();
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);
        ReflectionTestUtils.setField(biz, "rbacRoleBiz", roleBiz);
        return biz;
    }
}
