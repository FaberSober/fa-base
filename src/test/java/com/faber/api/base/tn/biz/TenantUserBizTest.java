package com.faber.api.base.tn.biz;

import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.entity.TenantUser;
import com.faber.api.base.tn.mapper.TenantUserMapper;
import com.faber.core.exception.BuzzException;
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

    private TenantUserBiz createBiz(TenantUserMapper mapper) {
        TenantUserBiz biz = new TenantUserBiz();
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);

        TenantBiz tenantBiz = mock(TenantBiz.class);
        when(tenantBiz.getById("tenant-1")).thenReturn(new Tenant());
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
