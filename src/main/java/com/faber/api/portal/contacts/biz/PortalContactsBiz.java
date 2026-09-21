package com.faber.api.portal.contacts.biz;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.biz.DepartmentBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.Department;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.api.portal.contacts.vo.PortalDepartmentNodeVo;
import com.faber.core.context.TenantContext;
import com.faber.core.vo.tree.TreeNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PortalContactsBiz {

    @Resource
    private DepartmentBiz departmentBiz;

    @Resource
    private TenantUserBiz tenantUserBiz;

    @Resource
    private UserBiz userBiz;

    public List<PortalDepartmentNodeVo> getDepartmentTree() {
        return toDepartmentTree(departmentBiz.allTree(), loadMemberCounts());
    }

    private Map<String, Long> loadMemberCounts() {
        String tenantId = TenantContext.getTenantId();
        var query = userBiz.lambdaQuery().eq(User::getStatus, true);
        if (StrUtil.isNotBlank(tenantId)) {
            List<String> tenantUserIds = tenantUserBiz.getUserIdsByTenantId(tenantId);
            if (tenantUserIds.isEmpty()) {
                return Collections.emptyMap();
            }
            query.in(User::getId, tenantUserIds);
        }
        return query.list().stream()
                .filter(user -> StrUtil.isNotBlank(user.getDepartmentId()))
                .collect(Collectors.groupingBy(User::getDepartmentId, Collectors.counting()));
    }

    static List<PortalDepartmentNodeVo> toDepartmentTree(
            List<TreeNode<Department>> nodes,
            Map<String, Long> memberCounts
    ) {
        if (nodes == null || nodes.isEmpty()) {
            return List.of();
        }
        return nodes.stream().map(node -> {
            PortalDepartmentNodeVo vo = new PortalDepartmentNodeVo();
            vo.setId(toString(node.getId()));
            vo.setParentId(toString(node.getParentId()));
            vo.setName(node.getName());
            vo.setSort(node.getSort());

            List<PortalDepartmentNodeVo> children = toDepartmentTree(node.getChildren(), memberCounts);
            vo.setHasChildren(!children.isEmpty());
            vo.setChildren(children);
            vo.setMemberCount(memberCounts.getOrDefault(vo.getId(), 0L).intValue());
            return vo;
        }).toList();
    }

    private static String toString(Object value) {
        return value == null ? null : value.toString();
    }
}
