package com.faber.api.portal.contacts.biz;

import com.faber.api.base.admin.entity.Department;
import com.faber.api.portal.contacts.vo.PortalDepartmentNodeVo;
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
}
