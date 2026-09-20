package com.faber.api.base.admin.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.faber.api.base.admin.entity.Department;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.mapper.DepartmentMapper;
import com.faber.api.base.admin.vo.ret.DepartmentExportVo;
import com.faber.api.base.admin.vo.ret.DepartmentVo;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaExcelUtils;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.vo.tree.TreeNode;
import com.faber.core.web.biz.BaseTreeBiz;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base-部门
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2020-06-07 19:26:53
 */
@Service
public class DepartmentBiz extends BaseTreeBiz<DepartmentMapper, Department> {

    @Resource
    private UserBiz userBiz;

    @Override
    protected void saveBefore(Department entity) {
        if (ObjectUtil.equal(entity.getParentId(), entity.getId())) {
            throw new BuzzException("父节点不能是自身");
        }
        if (entity.getId() != null && entity.getParentId() != null) {
            boolean parentIsDescendant = findChildren(entity.getId()).stream()
                    .anyMatch(item -> ObjectUtil.equal(item.getId(), entity.getParentId()));
            if (parentIsDescendant) {
                throw new BuzzException("父节点不能是当前部门或其下级部门");
            }
        }
    }

    @Override
    public boolean removeById(Serializable id) {
        List<Department> subtree = findAllChildren(id);
        if (subtree.isEmpty()) {
            throw new BuzzException("部门不存在，无法删除");
        }

        boolean hasChildren = subtree.stream()
                .anyMatch(item -> !ObjectUtil.equal(item.getId(), id));
        if (hasChildren) {
            throw new BuzzException("该部门包含子部门，无法删除，请先处理子部门");
        }

        List<String> departmentIds = subtree.stream()
                .map(Department::getId)
                .collect(Collectors.toList());
        long count = userBiz.lambdaQuery().in(User::getDepartmentId, departmentIds).count();
        if (count > 0) {
            throw new BuzzException("该部门或其下级部门仍有员工，无法删除，请先转移员工");
        }
        return super.removeById(id);
    }

    @Override
    public TableRet<Department> selectPageByQuery(QueryParams query) {
        TableRet<Department> table = super.selectPageByQuery(query);

        List<Department> list = table.getData().getRows().stream().map(this::decorate).collect(Collectors.toList());
        table.getData().setRows(list);

        return table;
    }

    @Override
    public List<TreeNode<Department>> getTree(QueryParams query) {
        List<TreeNode<Department>> tree = super.getTree(query);
        decorateTree(tree);
        return tree;
    }

    @Override
    public void exportExcel(QueryParams query) throws IOException {
        List<TreeNode<Department>> tree = super.getTree(query);
        if (hasExportFilters(query)) {
            Set<String> matchedIds = new HashSet<>();
            collectIds(tree, matchedIds);
            tree = filterTree(super.getTree(new QueryParams()), matchedIds);
        }
        Map<String, User> managers = findManagers(tree);
        List<DepartmentExportVo> list = flattenTree(tree, managers);
        FaExcelUtils.sendFileExcel(DepartmentExportVo.class, list);
    }

    private boolean hasExportFilters(QueryParams query) {
        return query != null && query.getQuery() != null && !query.getQuery().isEmpty();
    }

    private void collectIds(List<TreeNode<Department>> nodes, Set<String> ids) {
        if (nodes == null) {
            return;
        }
        for (TreeNode<Department> node : nodes) {
            ids.add(String.valueOf(node.getId()));
            collectIds(node.getChildren(), ids);
        }
    }

    private List<TreeNode<Department>> filterTree(List<TreeNode<Department>> nodes, Set<String> matchedIds) {
        List<TreeNode<Department>> result = new ArrayList<>();
        if (nodes == null) {
            return result;
        }
        for (TreeNode<Department> node : nodes) {
            List<TreeNode<Department>> children = filterTree(node.getChildren(), matchedIds);
            if (matchedIds.contains(String.valueOf(node.getId())) || !children.isEmpty()) {
                node.setChildren(children.isEmpty() ? null : children);
                node.setHasChildren(!children.isEmpty());
                result.add(node);
            }
        }
        return result;
    }

    private Map<String, User> findManagers(List<TreeNode<Department>> nodes) {
        Set<String> managerIds = new HashSet<>();
        collectManagerIds(nodes, managerIds);
        if (managerIds.isEmpty()) {
            return Map.of();
        }

        Map<String, User> managers = new HashMap<>();
        userBiz.getByIds(new ArrayList<>(managerIds)).forEach(user -> managers.put(user.getId(), user));
        return managers;
    }

    private void collectManagerIds(List<TreeNode<Department>> nodes, Set<String> managerIds) {
        if (nodes == null) {
            return;
        }
        for (TreeNode<Department> node : nodes) {
            Department department = node.getSourceData();
            if (department != null && department.getManagerId() != null) {
                managerIds.add(department.getManagerId());
            }
            collectManagerIds(node.getChildren(), managerIds);
        }
    }

    private List<DepartmentExportVo> flattenTree(List<TreeNode<Department>> nodes, Map<String, User> managers) {
        List<DepartmentExportVo> list = new ArrayList<>();
        if (nodes == null) {
            return list;
        }
        for (TreeNode<Department> node : nodes) {
            Department department = node.getSourceData();
            if (department == null) {
                continue;
            }
            list.add(DepartmentExportVo.from(department, node.getLevel(), managers.get(department.getManagerId())));
            list.addAll(flattenTree(node.getChildren(), managers));
        }
        return list;
    }

    private void decorateTree(List<TreeNode<Department>> nodes) {
        if (nodes == null) {
            return;
        }
        for (TreeNode<Department> node : nodes) {
            Department sourceData = node.getSourceData();
            if (sourceData != null) {
                node.setSourceData(decorate(sourceData));
            }
            decorateTree(node.getChildren());
        }
    }

    public DepartmentVo decorate(Department entity) {
        DepartmentVo vo = new DepartmentVo();
        BeanUtil.copyProperties(entity, vo);

        vo.setManager(userBiz.getByIdWithCache(vo.getManagerId()));
        return vo;
    }

    public Department getByNameWithCache(String name) {
        Map<Serializable, Department> cache = BaseContextHandler.getCacheMap("DepartmentBiz.getByNameWithCache");
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        Department entity = getTop(
                lambdaQuery()
                        .eq(Department::getName, name)
                        .orderByDesc(Department::getId)
        );
        cache.put(name, entity);
        return entity;
    }

    // 根据员工id获取部门负责人id
    public User getManagerByUserId(String userId) {
        Department department = getByIdWithCache(userBiz.getByIdWithCache(userId).getDepartmentId());
        return userBiz.getByIdWithCache(department.getManagerId());
    }

}
