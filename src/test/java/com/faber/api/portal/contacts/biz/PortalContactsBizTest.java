package com.faber.api.portal.contacts.biz;

import com.faber.api.base.admin.entity.Department;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.enums.UserWorkStatusEnum;
import com.faber.api.portal.contacts.vo.PortalDepartmentNodeVo;
import com.faber.api.portal.contacts.vo.PortalContactSummaryVo;
import com.faber.core.vo.tree.TreeNode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PortalContactsBizTest {

    @Test
    void mapsDepartmentTreeAndMemberCounts() {
        TreeNode<Department> root = new TreeNode<>();
        root.setId("root");
        root.setName("总部");

        TreeNode<Department> child = new TreeNode<>();
        child.setId("child");
        child.setParentId("root");
        child.setName("研发部");
        root.add(child);

        List<PortalDepartmentNodeVo> result = PortalContactsBiz.toDepartmentTree(
                List.of(root),
                Map.of("root", 2L, "child", 3L)
        );

        assertEquals(1, result.size());
        assertEquals("root", result.get(0).getId());
        assertEquals(2, result.get(0).getMemberCount());
        assertTrue(result.get(0).isHasChildren());
        assertEquals("研发部", result.get(0).getChildren().get(0).getName());
        assertEquals(3, result.get(0).getChildren().get(0).getMemberCount());
        assertFalse(result.get(0).getChildren().get(0).isHasChildren());
    }

    @Test
    void mapsOnlyContactSummaryFields() {
        User user = new User();
        user.setId("user-1");
        user.setName("张三");
        user.setImg("avatar.png");
        user.setDepartmentId("dept-1");
        user.setDepartmentName("研发部");
        user.setRoleNames("成员");
        user.setWorkStatus(UserWorkStatusEnum.ON_JOB);
        user.setTel("13800000000");
        user.setEmail("zhangsan@example.com");

        PortalContactSummaryVo result = PortalContactsBiz.toContactSummary(user);

        assertEquals("user-1", result.getId());
        assertEquals("张三", result.getName());
        assertEquals("avatar.png", result.getAvatar());
        assertEquals("研发部", result.getDepartmentName());
        assertEquals(UserWorkStatusEnum.ON_JOB, result.getWorkStatus());
    }
}
