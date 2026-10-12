package com.faber.api.base.admin.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.admin.entity.Department;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.mapper.DepartmentMapper;
import com.faber.api.base.admin.vo.query.DepartmentImportReqVo;
import com.faber.api.base.admin.vo.ret.DepartmentExportVo;
import com.faber.api.base.admin.vo.ret.DepartmentImportErrorVo;
import com.faber.api.base.admin.vo.ret.DepartmentImportPreviewVo;
import com.faber.api.base.admin.vo.ret.DepartmentImportResultVo;
import com.faber.api.base.admin.vo.ret.DepartmentImportRowVo;
import com.faber.api.base.admin.vo.ret.DepartmentVo;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaExcelUtils;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.vo.tree.TreeNode;
import com.faber.core.web.biz.BaseTreeBiz;
import org.dromara.x.file.storage.core.FileInfo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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

    private static final int DEPARTMENT_IMPORT_MAX_ROWS = 5000;
    private static final long DEPARTMENT_IMPORT_MAX_FILE_BYTES = 10 * 1024 * 1024L;
    private static final Map<String, String> DEPARTMENT_TYPE_ALIASES = Map.of(
            "CORP", "CORP",
            "DEPT", "DEPT",
            "TEAM", "TEAM",
            "公司", "CORP",
            "部门", "DEPT",
            "小组", "TEAM"
    );

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

    /** 下载部门专用导入模板。 */
    public void exportDepartmentImportTemplate() throws IOException {
        FaExcelUtils.sendFileExcel(DepartmentImportRowVo.class, List.of());
    }

    /** 读取并校验部门导入文件，只返回预览结果，不写入数据库。 */
    public DepartmentImportPreviewVo previewDepartmentImport(DepartmentImportReqVo request) {
        return buildDepartmentImportPlan(request).toPreview();
    }

    /** 重新校验部门导入文件后在同一事务中提交全部变更。 */
    @Transactional(rollbackFor = Exception.class)
    public DepartmentImportResultVo commitDepartmentImport(DepartmentImportReqVo request) {
        DepartmentImportPlan plan = buildDepartmentImportPlan(request);
        if (!plan.errors.isEmpty()) {
            throw new BuzzException("导入文件存在校验错误，请修正后重新预览");
        }

        if (!plan.creates.isEmpty()) {
            saveBatch(plan.creates);
        }
        if (!plan.updates.isEmpty()) {
            updateBatchById(plan.updates);
        }
        saveFileBiz("", "", "department-import", request.getFileId());

        DepartmentImportResultVo result = new DepartmentImportResultVo();
        result.setTotalCount(plan.creates.size() + plan.updates.size());
        result.setCreateCount(plan.creates.size());
        result.setUpdateCount(plan.updates.size());
        return result;
    }

    private DepartmentImportPlan buildDepartmentImportPlan(DepartmentImportReqVo request) {
        if (request == null || StrUtil.isBlank(request.getFileId())) {
            throw new BuzzException("导入文件不能为空");
        }

        List<DepartmentImportRowVo> rows = readDepartmentImportRows(request.getFileId());
        Map<String, Department> existingById = loadDepartments();
        List<DepartmentImportCandidate> candidates = new ArrayList<>();
        Map<String, Integer> firstIdRows = new HashMap<>();
        Map<String, Integer> firstNameRows = new HashMap<>();
        Map<String, String> existingNameKeys = new HashMap<>();

        existingById.values().forEach(department ->
                existingNameKeys.putIfAbsent(departmentNameKey(department.getParentId(), department.getName()), department.getId())
        );

        for (DepartmentImportRowVo row : rows) {
            DepartmentImportCandidate candidate = new DepartmentImportCandidate(row);
            String sourceId = normalize(row.getId());
            candidate.id = sourceId == null ? UUID.randomUUID().toString(true) : sourceId;
            candidate.parentId = normalizeParentId(row.getParentId());
            candidate.name = normalize(row.getName());
            candidate.type = parseDepartmentType(row.getType(), candidate.errors);
            candidate.managerId = normalize(row.getManagerId());
            candidate.sort = parseSort(row.getSort(), candidate.errors);
            candidate.update = existingById.containsKey(candidate.id);

            if (candidate.id.length() > 64) {
                candidate.errors.add("部门ID长度不能超过64个字符");
            }
            checkDuplicate(candidate.id, "部门ID", firstIdRows, candidate);
            if (candidate.name == null) {
                candidate.errors.add("部门名称不能为空");
            } else {
                String nameKey = departmentNameKey(candidate.parentId, candidate.name);
                Integer firstNameRow = firstNameRows.get(nameKey);
                if (firstNameRow != null) {
                    candidate.errors.add("同级部门名称重复（第" + firstNameRow + "行）");
                } else {
                    firstNameRows.put(nameKey, candidate.row.getRowNumber());
                }
                String existingId = existingNameKeys.get(nameKey);
                if (existingId != null && !Objects.equals(existingId, candidate.id)) {
                    candidate.errors.add("同级部门名称已存在");
                }
            }
            candidates.add(candidate);
        }

        Map<String, DepartmentImportCandidate> candidatesById = new LinkedHashMap<>();
        candidates.forEach(candidate -> candidatesById.putIfAbsent(candidate.id, candidate));
        Map<String, String> parentById = new HashMap<>();
        existingById.values().forEach(department -> parentById.put(department.getId(), normalizeParentId(department.getParentId())));
        candidates.forEach(candidate -> parentById.put(candidate.id, candidate.parentId));

        for (DepartmentImportCandidate candidate : candidates) {
            if (!"0".equals(candidate.parentId)
                    && !existingById.containsKey(candidate.parentId)
                    && !candidatesById.containsKey(candidate.parentId)) {
                candidate.errors.add("上级部门不存在");
            }
            if (Objects.equals(candidate.id, candidate.parentId)) {
                candidate.errors.add("父节点不能是自身");
            }
            validateParentChain(candidate, parentById);
        }

        Set<String> managerIds = candidates.stream()
                .map(candidate -> candidate.managerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<String, User> managersById = managerIds.isEmpty()
                ? Map.of()
                : userBiz.getByIds(new ArrayList<>(managerIds)).stream()
                        .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));

        DepartmentImportPlan plan = new DepartmentImportPlan(rows.size());
        for (DepartmentImportCandidate candidate : candidates) {
            if (candidate.managerId != null && !managersById.containsKey(candidate.managerId)) {
                candidate.errors.add("负责人不存在");
            }
            if (!candidate.errors.isEmpty()) {
                plan.errors.add(new DepartmentImportErrorVo(
                        candidate.row.getRowNumber(),
                        candidate.name,
                        String.join("；", candidate.errors)
                ));
                continue;
            }

            Department entity = new Department();
            entity.setId(candidate.id);
            entity.setParentId(candidate.parentId);
            entity.setName(candidate.name);
            entity.setType(candidate.type);
            entity.setManagerId(candidate.managerId);
            entity.setSort(candidate.sort);
            entity.setDescription(normalize(candidate.row.getDescription()));
            if (candidate.update) {
                plan.updates.add(entity);
            } else {
                plan.creates.add(entity);
            }
        }
        return plan;
    }

    private Map<String, Department> loadDepartments() {
        QueryWrapper<Department> wrapper = new QueryWrapper<>();
        addTenantQueryIfNeed(wrapper);
        return super.list(wrapper).stream()
                .collect(Collectors.toMap(Department::getId, department -> department, (left, right) -> left));
    }

    private List<DepartmentImportRowVo> readDepartmentImportRows(String fileId) {
        File file = validateDepartmentImportFile(fileId);
        List<DepartmentImportRowVo> rows = new ArrayList<>();
        try {
            EasyExcel.read(file, DepartmentImportRowVo.class, new AnalysisEventListener<DepartmentImportRowVo>() {
                @Override
                public void invoke(DepartmentImportRowVo data, AnalysisContext context) {
                    if (data == null || isEmptyImportRow(data)) {
                        return;
                    }
                    if (rows.size() >= DEPARTMENT_IMPORT_MAX_ROWS) {
                        throw new BuzzException("单次最多导入" + DEPARTMENT_IMPORT_MAX_ROWS + "行");
                    }
                    data.setRowNumber(context.readRowHolder().getRowIndex() + 1);
                    rows.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                }
            }).sheet().doRead();
        } catch (BuzzException e) {
            throw e;
        } catch (Exception e) {
            throw new BuzzException("部门导入文件读取失败，请使用下载的部门导入模板");
        }
        if (rows.isEmpty()) {
            throw new BuzzException("导入文件没有可用数据");
        }
        return rows;
    }

    private File validateDepartmentImportFile(String fileId) {
        File file = getFileById(fileId);
        if (file == null || !file.isFile()) {
            throw new BuzzException("导入文件不存在或已失效");
        }
        if (file.length() <= 0 || file.length() > DEPARTMENT_IMPORT_MAX_FILE_BYTES) {
            throw new BuzzException("导入文件大小必须大于0且不超过10MB");
        }
        FileInfo fileInfo = getFileInfoById(fileId);
        String ext = fileInfo == null ? null : fileInfo.getExt();
        if (!"xls".equalsIgnoreCase(ext) && !"xlsx".equalsIgnoreCase(ext)) {
            throw new BuzzException("仅支持导入 .xls 或 .xlsx 文件");
        }
        return file;
    }

    private void validateParentChain(DepartmentImportCandidate candidate, Map<String, String> parentById) {
        Set<String> visited = new HashSet<>();
        String currentId = candidate.id;
        while (!"0".equals(currentId)) {
            if (!visited.add(currentId)) {
                candidate.errors.add("父级关系存在循环");
                return;
            }
            String parentId = parentById.get(currentId);
            if (parentId == null || "0".equals(parentId)) {
                return;
            }
            currentId = parentId;
        }
    }

    private String parseDepartmentType(String value, List<String> errors) {
        String normalized = normalize(value);
        if (normalized == null) {
            errors.add("部门类型不能为空");
            return null;
        }
        String type = DEPARTMENT_TYPE_ALIASES.get(normalized.toUpperCase(Locale.ROOT));
        if (type == null) {
            type = DEPARTMENT_TYPE_ALIASES.get(normalized);
        }
        if (type == null) {
            errors.add("部门类型只能填写 CORP、DEPT、TEAM 或 公司、部门、小组");
        }
        return type;
    }

    private Integer parseSort(String value, List<String> errors) {
        String normalized = normalize(value);
        if (normalized == null) {
            return 0;
        }
        try {
            int sort = Integer.parseInt(normalized);
            if (sort < 0) {
                errors.add("排序不能小于0");
                return 0;
            }
            return sort;
        } catch (NumberFormatException e) {
            errors.add("排序必须是非负整数");
            return 0;
        }
    }

    private void checkDuplicate(String value, String label, Map<String, Integer> firstRows,
                                DepartmentImportCandidate candidate) {
        Integer firstRow = firstRows.putIfAbsent(value, candidate.row.getRowNumber());
        if (firstRow != null) {
            candidate.errors.add(label + "重复（第" + firstRow + "行）");
        }
    }

    private String departmentNameKey(String parentId, String name) {
        String normalizedName = normalize(name);
        return normalizeParentId(parentId) + "\u0000" + (normalizedName == null ? "" : normalizedName.toLowerCase(Locale.ROOT));
    }

    private String normalizeParentId(String value) {
        String normalized = normalize(value);
        return normalized == null || "根节点".equals(normalized) ? "0" : normalized;
    }

    private boolean isEmptyImportRow(DepartmentImportRowVo row) {
        return StrUtil.isAllBlank(row.getId(), row.getParentId(), row.getName(), row.getType(), row.getManagerId(), row.getSort(), row.getDescription());
    }

    private String normalize(String value) {
        return StrUtil.trimToNull(value);
    }

    private static class DepartmentImportCandidate {
        private final DepartmentImportRowVo row;
        private final List<String> errors = new ArrayList<>();
        private String id;
        private String parentId;
        private String name;
        private String type;
        private String managerId;
        private Integer sort;
        private boolean update;

        private DepartmentImportCandidate(DepartmentImportRowVo row) {
            this.row = row;
        }
    }

    private static class DepartmentImportPlan {
        private final int totalCount;
        private final List<Department> creates = new ArrayList<>();
        private final List<Department> updates = new ArrayList<>();
        private final List<DepartmentImportErrorVo> errors = new ArrayList<>();

        private DepartmentImportPlan(int totalCount) {
            this.totalCount = totalCount;
        }

        private DepartmentImportPreviewVo toPreview() {
            DepartmentImportPreviewVo preview = new DepartmentImportPreviewVo();
            preview.setTotalCount(totalCount);
            preview.setValidCount(creates.size() + updates.size());
            preview.setErrorCount(errors.size());
            preview.setCreateCount(creates.size());
            preview.setUpdateCount(updates.size());
            preview.setErrors(errors);
            return preview;
        }
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
