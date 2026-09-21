package com.faber.api.portal.contacts.biz;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.biz.DepartmentBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.Department;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.api.portal.contacts.vo.PortalContactPageQueryVo;
import com.faber.api.portal.contacts.vo.PortalDepartmentNodeVo;
import com.faber.api.portal.contacts.vo.PortalContactSummaryVo;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.context.TenantContext;
import com.faber.core.vo.query.BasePageQuery;
import com.faber.core.vo.query.Condition;
import com.faber.core.vo.query.ConditionGroup;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.vo.query.enums.ConditionGroupTypeEnum;
import com.faber.core.vo.query.enums.ConditionOprEnum;
import com.faber.core.vo.tree.TreeNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PortalContactsBiz {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String INVALID_DEPARTMENT_ID = "__not_current_tenant_department__";

    @Resource
    private DepartmentBiz departmentBiz;

    @Resource
    private TenantUserBiz tenantUserBiz;

    @Resource
    private UserBiz userBiz;

    public List<PortalDepartmentNodeVo> getDepartmentTree() {
        return toDepartmentTree(departmentBiz.allTree(), loadMemberCounts());
    }

    public TableRet<PortalContactSummaryVo> pageUsers(BasePageQuery<PortalContactPageQueryVo> request) {
        int current = request == null ? 1 : Math.max(1, request.getCurrent());
        int pageSize = request == null
                ? DEFAULT_PAGE_SIZE
                : Math.min(MAX_PAGE_SIZE, Math.max(1, request.getPageSize()));
        PortalContactPageQueryVo filter = request == null ? null : request.getQuery();
        String keyword = filter == null ? null : StrUtil.trim(filter.getKeyword());
        String departmentId = filter == null ? null : StrUtil.trim(filter.getDepartmentId());

        QueryParams query = new QueryParams();
        query.setCurrent(current);
        query.setPageSize(pageSize);
        query.setSorter("name asc");
        Map<String, Object> queryMap = new HashMap<>();
        queryMap.put("status", true);
        query.setQuery(queryMap);

        List<TreeNode<Department>> departmentTree = List.of();
        if (StrUtil.isNotBlank(departmentId) || StrUtil.isNotBlank(keyword)) {
            departmentTree = departmentBiz.allTree();
        }
        Set<String> departmentIds = departmentIds(departmentTree);
        if (StrUtil.isNotBlank(departmentId)) {
            queryMap.put("departmentId", departmentIds.contains(departmentId)
                    ? departmentId : INVALID_DEPARTMENT_ID);
        }
        if (StrUtil.isNotBlank(keyword)) {
            query.addConditionGroup(keywordConditions(keyword, matchingDepartmentIds(departmentTree, keyword)));
        }

        TableRet<User> page = userBiz.selectPageByQuery(query);
        List<PortalContactSummaryVo> rows = page.getData().getRows().stream()
                .map(PortalContactsBiz::toContactSummary)
                .toList();
        return new TableRet<>(page.getData().getPagination(), rows);
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

    static PortalContactSummaryVo toContactSummary(User user) {
        PortalContactSummaryVo vo = new PortalContactSummaryVo();
        vo.setId(user.getId());
        vo.setName(user.getName());
        vo.setAvatar(user.getImg());
        vo.setDepartmentId(user.getDepartmentId());
        vo.setDepartmentName(user.getDepartmentName());
        vo.setRoleNames(user.getRoleNames());
        vo.setWorkStatus(user.getWorkStatus());
        return vo;
    }

    private static ConditionGroup keywordConditions(String keyword, List<String> departmentIds) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(condition("name", ConditionOprEnum.LIKE, keyword));
        conditions.add(condition("username", ConditionOprEnum.LIKE, keyword));
        if (!departmentIds.isEmpty()) {
            conditions.add(condition("departmentId", ConditionOprEnum.IN, departmentIds));
        }
        ConditionGroup group = new ConditionGroup();
        group.setType(ConditionGroupTypeEnum.OR);
        group.setCondList(conditions);
        return group;
    }

    private static Condition condition(String key, ConditionOprEnum opr, Object value) {
        Condition condition = new Condition();
        condition.setKey(key);
        condition.setOpr(opr);
        condition.setValue(value);
        return condition;
    }

    private static Set<String> departmentIds(List<TreeNode<Department>> nodes) {
        Set<String> ids = new HashSet<>();
        collectDepartmentIds(nodes, null, ids);
        return ids;
    }

    private static List<String> matchingDepartmentIds(List<TreeNode<Department>> nodes, String keyword) {
        Set<String> ids = new HashSet<>();
        collectDepartmentIds(nodes, keyword, ids);
        return ids.stream().toList();
    }

    private static void collectDepartmentIds(
            List<TreeNode<Department>> nodes,
            String keyword,
            Set<String> ids
    ) {
        if (nodes == null) {
            return;
        }
        for (TreeNode<Department> node : nodes) {
            if (StrUtil.isBlank(keyword) || StrUtil.containsIgnoreCase(node.getName(), keyword)) {
                ids.add(toString(node.getId()));
            }
            collectDepartmentIds(node.getChildren(), keyword, ids);
        }
    }

    private static String toString(Object value) {
        return value == null ? null : value.toString();
    }
}
