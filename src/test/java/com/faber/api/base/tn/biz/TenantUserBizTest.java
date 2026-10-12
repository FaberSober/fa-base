package com.faber.api.base.tn.biz;

import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.entity.TenantUser;
import com.faber.api.base.tn.mapper.TenantUserMapper;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantUserBizTest {

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
        TenantContext.clear();
    }

    @Test
    void restoresLogicallyDeletedAssociationOnSave() {
        TenantUserMapper mapper = mock(TenantUserMapper.class);
        TenantUserBiz biz = createBiz(mapper);
        TenantUser deletedAssociation = association("association-1", true);
        TenantUser request = association(null, false);
        request.setIsAdmin(true);
        request.setStatus(true);
        request.setSort(3);
        request.setDescription("restored");

        when(mapper.selectByTenantIdAndUserIdIgnoreLogic("tenant-1", "user-1"))
                .thenReturn(deletedAssociation);
        when(mapper.updateByIdIgnoreLogic(deletedAssociation)).thenReturn(1);

        assertTrue(biz.save(request));

        assertEquals("association-1", request.getId());
        assertFalse(deletedAssociation.getDeleted());
        assertEquals(Boolean.TRUE, deletedAssociation.getIsAdmin());
        assertEquals(Boolean.TRUE, deletedAssociation.getStatus());
        assertEquals(3, deletedAssociation.getSort());
        assertEquals("restored", deletedAssociation.getDescription());
        verify(mapper).updateByIdIgnoreLogic(deletedAssociation);
        verify(mapper, never()).insert(any(TenantUser.class));
    }

    @Test
    void rejectsDuplicateActiveAssociationOnSave() {
        TenantUserMapper mapper = mock(TenantUserMapper.class);
        TenantUserBiz biz = createBiz(mapper);
        TenantUser activeAssociation = association("association-1", false);

        when(mapper.selectByTenantIdAndUserIdIgnoreLogic("tenant-1", "user-1"))
                .thenReturn(activeAssociation);

        BuzzException exception = assertThrows(BuzzException.class,
                () -> biz.save(association(null, false)));

        assertEquals("该用户已关联到当前租户", exception.getMessage());
        verify(mapper, never()).updateByIdIgnoreLogic(any(TenantUser.class));
        verify(mapper, never()).insert(any(TenantUser.class));
    }

    @Test
    void rejectsInactiveTenantForMembershipLookup() {
        TenantUserMapper mapper = mock(TenantUserMapper.class);
        Tenant tenant = new Tenant();
        tenant.setStatus(false);
        TenantUserBiz biz = createBiz(mapper, tenant);

        assertFalse(biz.hasUserTenant("user-1", "tenant-1"));
    }

    @Test
    void rejectsExpiredTenantForMembershipLookup() {
        TenantUserMapper mapper = mock(TenantUserMapper.class);
        Tenant tenant = new Tenant();
        tenant.setStatus(true);
        tenant.setExpireTime(new java.util.Date(System.currentTimeMillis() - 1000));
        TenantUserBiz biz = createBiz(mapper, tenant);

        assertFalse(biz.hasUserTenant("user-1", "tenant-1"));
    }

    @Test
    void promotesExistingAssociationToTenantAdmin() {
        TenantUserMapper mapper = mock(TenantUserMapper.class);
        TenantUserBiz biz = createBiz(mapper);
        TenantUser association = association("association-1", false);

        when(mapper.selectByTenantIdAndUserIdIgnoreLogic("tenant-1", "user-1"))
                .thenReturn(association);
        when(mapper.updateByIdIgnoreLogic(association)).thenReturn(1);

        biz.ensureTenantAdmin("tenant-1", "user-1");

        assertTrue(association.getIsAdmin());
        assertTrue(association.getStatus());
        verify(mapper).updateByIdIgnoreLogic(association);
    }

    @Test
    void rejectsChangingAssociationTenantOrUserOnUpdate() {
        TenantUserMapper mapper = mock(TenantUserMapper.class);
        TenantUserBiz biz = createBiz(mapper);
        TenantUser existing = association("association-1", false);
        TenantUser request = association("association-1", false);
        request.setTenantId("tenant-2");
        BaseContextHandler.setUserId("1");

        when(mapper.selectByIdIgnoreLogic("association-1")).thenReturn(existing);

        BuzzException exception = assertThrows(BuzzException.class, () -> biz.updateById(request));

        assertEquals("租户用户关联的租户和用户不可修改", exception.getMessage());
        verify(mapper, never()).updateById(any(TenantUser.class));
    }

    @Test
    void rejectsMemberSaveOutsideCurrentTenant() {
        TenantUserMapper mapper = mock(TenantUserMapper.class);
        TenantUserBiz biz = createBiz(mapper, new Tenant(), true);
        BaseContextHandler.setUserId("user-1");
        TenantContext.setTenantId("tenant-2");

        BuzzException exception = assertThrows(BuzzException.class,
                () -> biz.save(association(null, false)));

        assertEquals("无权管理当前租户成员", exception.getMessage());
        verify(mapper, never()).insert(any(TenantUser.class));
    }

    private TenantUserBiz createBiz(TenantUserMapper mapper) {
        return createBiz(mapper, new Tenant());
    }

    private TenantUserBiz createBiz(TenantUserMapper mapper, Tenant tenant) {
        return createBiz(mapper, tenant, false);
    }

    private TenantUserBiz createBiz(TenantUserMapper mapper, Tenant tenant, boolean tenantEnabled) {
        TenantUserBiz biz = new TenantUserBiz();
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);

        FaSetting faSetting = new FaSetting();
        FaSetting.Tenant tenantSetting = new FaSetting.Tenant();
        tenantSetting.setEnabled(tenantEnabled);
        faSetting.setTenant(tenantSetting);
        ReflectionTestUtils.setField(biz, "faSetting", faSetting);

        TenantBiz tenantBiz = mock(TenantBiz.class);
        when(tenantBiz.getById("tenant-1")).thenReturn(tenant);
        ReflectionTestUtils.setField(biz, "tenantBiz", tenantBiz);

        UserBiz userBiz = mock(UserBiz.class);
        when(userBiz.getById("user-1")).thenReturn(new User());
        ReflectionTestUtils.setField(biz, "userBiz", userBiz);
        return biz;
    }

    private TenantUser association(String id, boolean deleted) {
        TenantUser association = new TenantUser();
        association.setId(id);
        association.setTenantId("tenant-1");
        association.setUserId("user-1");
        association.setIsAdmin(false);
        association.setStatus(false);
        association.setSort(0);
        association.setDeleted(deleted);
        return association;
    }
}
