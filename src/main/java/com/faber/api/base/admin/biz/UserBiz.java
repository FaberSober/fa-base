package com.faber.api.base.admin.biz;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.IterUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
//import com.alicp.jetcache.anno.CacheInvalidate;
//import com.alicp.jetcache.anno.Cached;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.faber.api.base.admin.entity.Department;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.entity.UserToken;
import com.faber.api.base.admin.mapper.UserMapper;
import com.faber.api.base.admin.vo.query.*;
import com.faber.api.base.admin.enums.UserWorkStatusEnum;
import com.faber.api.base.admin.vo.ret.UserExportVo;
import com.faber.api.base.admin.vo.ret.UserImportErrorVo;
import com.faber.api.base.admin.vo.ret.UserImportPreviewVo;
import com.faber.api.base.admin.vo.ret.UserImportResultVo;
import com.faber.api.base.admin.vo.ret.UserImportRowVo;
import com.faber.api.base.rbac.biz.RbacRoleBiz;
import com.faber.api.base.rbac.biz.RbacUserRoleBiz;
import com.faber.api.base.rbac.entity.RbacRole;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.config.utils.user.UserCheckUtil;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.constant.CommonConstants;
import com.faber.core.constant.FaSetting;
import com.faber.core.config.validator.validator.ValidUtils;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.enums.SexEnum;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.NoDataException;
import com.faber.core.exception.auth.UserInvalidException;
import com.faber.core.utils.FaExcelUtils;
import com.faber.core.utils.FaPwdUtils;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.vo.tree.TreeNode;
import com.faber.core.web.biz.BaseBiz;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.dromara.x.file.storage.core.FileInfo;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 用户
 *
 * @author wanghaobin
 * @create 2017-06-08 16:23
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class UserBiz extends BaseBiz<UserMapper, User> {

    private static final int USER_IMPORT_MAX_ROWS = 5000;
    private static final long USER_IMPORT_MAX_FILE_BYTES = 10 * 1024 * 1024L;
    private static final String USER_IMPORT_DEPARTMENT_SEPARATOR = "/";

    @Lazy
    @Resource
    private DepartmentBiz departmentBiz;

    @Lazy
    @Resource
    private RbacUserRoleBiz rbacUserRoleBiz;

    @Lazy
    @Resource
    private RbacRoleBiz rbacRoleBiz;

    @Lazy
    @Resource
    private UserTokenBiz userTokenBiz;

    @Lazy
    @Resource
    private TenantUserBiz tenantUserBiz;

    @Resource
    private FaSetting faSetting;

    @Autowired
    private RedissonClient redisson;

    @Value("${spring.data.redis.prefix}")
    private String redisPrefix;

    /**
     * 登录账户
     *
     * @param account
     * @param password
     * @return
     */
    public User validate(String account, String password) {
        User user = this.getUserByUsername(account);
        if (user == null) {
            user = this.getUserByTel(account);
        }
        UserCheckUtil.checkUserValid(user);

        if (!FaPwdUtils.checkPwd(password, user.getPassword())) {
            throw new UserInvalidException("用户名或密码错误！");
        }
        return user;
    }

    /**
     * 验证当前登录账户的密码是否正确
     *
     * @param password
     */
    public void validateCurrentUserPwd(String password) {
        User user = this.getByIdWithCache(getCurrentUserId());
        UserCheckUtil.checkUserValid(user);
        if (FaPwdUtils.checkPwd(password, user.getPassword())) {
            return;
        }
        throw new UserInvalidException("本账户密码验证失败");
    }

    public User getLoginUser() {
        User user = getById(getCurrentUserId());
        if (!user.getStatus()) throw new BuzzException("无效账户");
        this.decorateOne(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public TableRet<User> selectPageByQuery(QueryParams query) {
        appendTenantUserQueryIfNeed(query);
        return super.selectPageByQuery(query);
    }

    @Override
    public void exportExcel(QueryParams query) throws IOException {
        appendTenantUserQueryIfNeed(query);
        exportUsers(query);
    }

    public void exportExcelSuper(QueryParams query) throws IOException {
        if (!isSuperAdminUser(getCurrentUserId())) {
            throw new BuzzException("仅平台管理员可导出全部用户");
        }
        exportUsers(query);
    }

    private void exportUsers(QueryParams query) throws IOException {
        List<UserExportVo> list = this.list(query).stream()
                .map(UserExportVo::from)
                .collect(Collectors.toList());
        FaExcelUtils.sendFileExcel(UserExportVo.class, list);
    }

    /** 下载用户专用导入模板。 */
    public void exportUserImportTemplate() throws IOException {
        FaExcelUtils.sendFileExcel(UserImportRowVo.class, List.of());
    }

    /** 读取并校验用户导入文件，只返回预览结果，不写入数据库。 */
    public UserImportPreviewVo previewUserImport(UserImportReqVo request) {
        return buildUserImportPlan(request).toPreview();
    }

    /** 重新校验导入文件后在同一事务中提交全部用户变更。 */
    public UserImportResultVo commitUserImport(UserImportReqVo request) {
        UserImportPlan plan = buildUserImportPlan(request);
        if (!plan.errors.isEmpty()) {
            throw new BuzzException("导入文件存在校验错误，请修正后重新预览");
        }

        for (UserImportChange change : plan.changes) {
            if (change.update) {
                updateById(change.entity);
            } else {
                save(change.entity);
            }
        }
        saveFileBiz("", "", "user-import", request.getFileId());

        UserImportResultVo result = new UserImportResultVo();
        result.setTotalCount(plan.changes.size());
        result.setCreateCount(plan.createCount);
        result.setUpdateCount(plan.updateCount);
        return result;
    }

    private UserImportPlan buildUserImportPlan(UserImportReqVo request) {
        if (request == null || StrUtil.isBlank(request.getFileId())) {
            throw new BuzzException("导入文件不能为空");
        }

        List<UserImportRowVo> rows = readUserImportRows(request.getFileId());
        ImportScope scope = buildImportScope(request);
        DepartmentIndex departmentIndex = loadDepartmentIndex();
        Map<String, List<RbacRole>> rolesByName = rbacRoleBiz.list().stream()
                .filter(role -> StrUtil.isNotBlank(role.getName()))
                .collect(Collectors.groupingBy(RbacRole::getName));

        List<String> ids = rows.stream()
                .map(row -> normalize(row.getId()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, User> existingById = loadExistingUsers(ids, scope);

        List<String> usernames = rows.stream()
                .map(row -> normalize(row.getUsername()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<String> tels = rows.stream()
                .map(row -> normalize(row.getTel()))
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        List<User> duplicateCandidates = loadDuplicateCandidates(usernames, tels);
        Map<String, List<User>> usersByUsername = duplicateCandidates.stream()
                .filter(user -> StrUtil.isNotBlank(user.getUsername()))
                .collect(Collectors.groupingBy(User::getUsername));
        Map<String, List<User>> usersByTel = duplicateCandidates.stream()
                .filter(user -> StrUtil.isNotBlank(user.getTel()))
                .collect(Collectors.groupingBy(User::getTel));

        UserImportPlan plan = new UserImportPlan(rows.size());
        Map<String, Integer> firstIdRows = new HashMap<>();
        Map<String, Integer> firstUsernameRows = new HashMap<>();
        Map<String, Integer> firstTelRows = new HashMap<>();

        for (UserImportRowVo row : rows) {
            List<String> errors = new ArrayList<>();
            String id = normalize(row.getId());
            String username = normalize(row.getUsername());
            String name = normalize(row.getName());
            String departmentText = normalize(row.getDepartment());
            String tel = normalize(row.getTel());
            String email = normalize(row.getEmail());
            String password = normalize(row.getPassword());
            User existing = id == null ? null : existingById.get(id);

            required(username, "用户名", errors);
            required(name, "姓名", errors);
            required(departmentText, "部门", errors);
            required(tel, "手机号", errors);
            if (tel != null && !ValidUtils.validTelNo(tel)) {
                errors.add("手机号格式错误");
            }
            if (email != null && !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                errors.add("邮箱格式错误");
            }

            Boolean status = parseBoolean(row.getStatus(), "账户有效", errors);
            Boolean adminEnabled = parseBoolean(row.getAdminEnabled(), "允许访问后台", errors);
            SexEnum sex = parseSex(row.getSex(), errors);
            UserWorkStatusEnum workStatus = parseWorkStatus(row.getWorkStatus(), errors);
            Date birthday = parseBirthday(row.getBirthday(), errors);

            if (id != null) {
                checkDuplicate(id, "用户ID", firstIdRows, row.getRowNumber(), errors);
                if (existing == null) {
                    errors.add("用户ID不存在或不在当前可操作范围");
                }
                if (password != null) {
                    errors.add("更新用户不能填写初始密码");
                }
            } else if (password == null) {
                errors.add("新增用户必须填写初始密码");
            }
            checkDuplicate(username, "用户名", firstUsernameRows, row.getRowNumber(), errors);
            checkDuplicate(tel, "手机号", firstTelRows, row.getRowNumber(), errors);
            checkUniqueConflict(username, id, usersByUsername, "用户名", errors);
            checkUniqueConflict(tel, id, usersByTel, "手机号", errors);

            Department department = resolveDepartment(departmentText, departmentIndex, errors);
            if (department != null && !scope.allowsDepartment(department.getId())) {
                errors.add("部门不在当前可操作范围内");
            }
            List<Long> roleIds = resolveRoles(row.getRoleNames(), rolesByName, errors);

            if (!errors.isEmpty()) {
                plan.errors.add(new UserImportErrorVo(row.getRowNumber(), username, String.join("；", errors)));
                continue;
            }

            User entity = new User();
            entity.setId(id);
            entity.setUsername(username);
            entity.setName(name);
            entity.setDepartmentId(department.getId());
            entity.setRoleIds(roleIds);
            entity.setTel(tel);
            entity.setEmail(email);
            entity.setSex(sex);
            entity.setWorkStatus(workStatus);
            entity.setStatus(status);
            entity.setAdminEnabled(adminEnabled);
            entity.setBirthday(birthday);
            entity.setAddress(normalize(row.getAddress()));
            entity.setDescription(normalize(row.getDescription()));
            entity.setWxUnionId(normalize(row.getWxUnionId()));
            entity.setWxMaOpenid(normalize(row.getWxMaOpenid()));
            entity.setPassword(password);
            plan.changes.add(new UserImportChange(entity, existing != null));
            if (existing == null) {
                plan.createCount++;
            } else {
                plan.updateCount++;
            }
        }
        return plan;
    }

    private File validateImportFile(String fileId) {
        File file = getFileById(fileId);
        if (file == null || !file.isFile()) {
            throw new BuzzException("导入文件不存在或已失效");
        }
        if (file.length() <= 0 || file.length() > USER_IMPORT_MAX_FILE_BYTES) {
            throw new BuzzException("导入文件大小必须大于0且不超过10MB");
        }
        FileInfo fileInfo = getFileInfoById(fileId);
        String ext = fileInfo == null ? null : fileInfo.getExt();
        if (!"xls".equalsIgnoreCase(ext) && !"xlsx".equalsIgnoreCase(ext)) {
            throw new BuzzException("仅支持导入 .xls 或 .xlsx 文件");
        }
        return file;
    }

    private List<UserImportRowVo> readUserImportRows(String fileId) {
        File file = validateImportFile(fileId);
        List<UserImportRowVo> rows = new ArrayList<>();
        try {
            EasyExcel.read(file, UserImportRowVo.class, new AnalysisEventListener<UserImportRowVo>() {
                @Override
                public void invoke(UserImportRowVo data, AnalysisContext context) {
                    if (data == null || isEmptyImportRow(data)) {
                        return;
                    }
                    if (rows.size() >= USER_IMPORT_MAX_ROWS) {
                        throw new BuzzException("单次最多导入" + USER_IMPORT_MAX_ROWS + "行");
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
            throw new BuzzException("用户导入文件读取失败，请使用下载的用户导入模板");
        }
        if (rows.isEmpty()) {
            throw new BuzzException("导入文件没有可用数据");
        }
        return rows;
    }

    private ImportScope buildImportScope(UserImportReqVo request) {
        boolean superMode = Boolean.TRUE.equals(request.getSuperMode());
        String currentUserId = getCurrentUserId();
        if (superMode && !isSuperAdminUser(currentUserId)) {
            throw new BuzzException("仅平台管理员可使用跨租户用户导入");
        }

        Set<String> visibleUserIds = null;
        if (!superMode && faSetting.isTenantEnabled()) {
            String tenantId = TenantContext.getTenantId();
            if (StrUtil.isBlank(tenantId) && !isSuperAdminUser(currentUserId)) {
                tenantId = TenantContext.requireTenantId();
            }
            if (StrUtil.isNotBlank(tenantId)) {
                visibleUserIds = new HashSet<>(tenantUserBiz.getUserIdsByTenantId(tenantId));
            }
        }

        Set<String> departmentIds = null;
        String departmentId = normalize(request.getDepartmentIdSuper());
        if (StrUtil.isNotBlank(departmentId) && !superMode) {
            List<Department> departments = departmentBiz.findAllChildren(departmentId);
            if (departments.isEmpty()) {
                throw new BuzzException("当前部门范围不存在");
            }
            departmentIds = departments.stream().map(Department::getId).collect(Collectors.toSet());
        }
        return new ImportScope(visibleUserIds, departmentIds);
    }

    private DepartmentIndex loadDepartmentIndex() {
        DepartmentIndex index = new DepartmentIndex();
        List<TreeNode<Department>> tree = departmentBiz.allTree();
        if (tree != null) {
            tree.forEach(node -> indexDepartments(node, "", index));
        }
        return index;
    }

    private void indexDepartments(TreeNode<Department> node, String parentPath, DepartmentIndex index) {
        if (node == null || node.getSourceData() == null) {
            return;
        }
        String name = normalize(node.getName());
        if (name == null) {
            return;
        }
        String path = StrUtil.isBlank(parentPath) ? name : parentPath + USER_IMPORT_DEPARTMENT_SEPARATOR + name;
        Department department = node.getSourceData();
        index.byPath.computeIfAbsent(normalizePath(path), key -> new ArrayList<>()).add(department);
        index.byName.computeIfAbsent(name, key -> new ArrayList<>()).add(department);
        if (node.getChildren() != null) {
            node.getChildren().forEach(child -> indexDepartments(child, path, index));
        }
    }

    private Department resolveDepartment(String value, DepartmentIndex index, List<String> errors) {
        if (value == null) {
            return null;
        }
        List<Department> matches = index.byPath.get(normalizePath(value));
        if (matches == null || matches.isEmpty()) {
            matches = index.byName.get(value);
        }
        if (matches == null || matches.isEmpty()) {
            errors.add("部门不存在，请填写完整部门路径");
            return null;
        }
        if (matches.size() > 1) {
            errors.add("部门名称不唯一，请填写完整部门路径");
            return null;
        }
        return matches.get(0);
    }

    private List<Long> resolveRoles(String roleText, Map<String, List<RbacRole>> rolesByName, List<String> errors) {
        String value = normalize(roleText);
        if (value == null) {
            errors.add("角色名称不能为空");
            return List.of();
        }

        Set<Long> roleIds = new LinkedHashSet<>();
        Set<String> roleNames = new HashSet<>();
        for (String rawName : value.split("[,，]")) {
            String roleName = normalize(rawName);
            if (roleName == null) {
                errors.add("角色名称不能为空");
                continue;
            }
            if (!roleNames.add(roleName)) {
                errors.add("角色名称重复：" + roleName);
                continue;
            }
            List<RbacRole> matches = rolesByName.get(roleName);
            if (matches == null || matches.isEmpty()) {
                errors.add("角色不存在或不在当前可用范围：" + roleName);
                continue;
            }
            if (matches.size() > 1) {
                errors.add("角色名称不唯一：" + roleName);
                continue;
            }
            if (Boolean.FALSE.equals(matches.get(0).getStatus())) {
                errors.add("角色未启用：" + roleName);
                continue;
            }
            roleIds.add(matches.get(0).getId());
        }
        return new ArrayList<>(roleIds);
    }

    private Map<String, User> loadExistingUsers(List<String> ids, ImportScope scope) {
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return lambdaQuery().in(User::getId, ids).list().stream()
                .filter(scope::allowsUser)
                .collect(Collectors.toMap(User::getId, user -> user, (left, right) -> left));
    }

    private List<User> loadDuplicateCandidates(List<String> usernames, List<String> tels) {
        List<User> users = new ArrayList<>();
        if (!usernames.isEmpty()) {
            users.addAll(lambdaQuery().in(User::getUsername, usernames).list());
        }
        if (!tels.isEmpty()) {
            users.addAll(lambdaQuery().in(User::getTel, tels).list());
        }
        return users;
    }

    private void checkUniqueConflict(String value, String id, Map<String, List<User>> usersByValue,
                                     String label, List<String> errors) {
        if (value == null) {
            return;
        }
        boolean conflict = usersByValue.getOrDefault(value, List.of()).stream()
                .anyMatch(user -> !Objects.equals(user.getId(), id));
        if (conflict) {
            errors.add(label + "已存在");
        }
    }

    private void checkDuplicate(String value, String label, Map<String, Integer> firstRows,
                                int rowNumber, List<String> errors) {
        if (value == null) {
            return;
        }
        Integer firstRow = firstRows.putIfAbsent(value, rowNumber);
        if (firstRow != null) {
            errors.add(label + "重复（第" + firstRow + "行）");
        }
    }

    private Boolean parseBoolean(String value, String label, List<String> errors) {
        value = normalize(value);
        if (value == null) {
            errors.add(label + "不能为空");
            return null;
        }
        if ("是".equals(value)) {
            return true;
        }
        if ("否".equals(value)) {
            return false;
        }
        errors.add(label + "只能填写“是”或“否”");
        return null;
    }

    private SexEnum parseSex(String value, List<String> errors) {
        value = normalize(value);
        if (value == null) {
            return null;
        }
        for (SexEnum item : SexEnum.values()) {
            if (item.getDesc().equals(value)) {
                return item;
            }
        }
        errors.add("性别只能填写：女、男或保密");
        return null;
    }

    private UserWorkStatusEnum parseWorkStatus(String value, List<String> errors) {
        value = normalize(value);
        if (value == null) {
            errors.add("工作状态不能为空");
            return null;
        }
        for (UserWorkStatusEnum item : UserWorkStatusEnum.values()) {
            if (item.getDesc().equals(value)) {
                return item;
            }
        }
        errors.add("工作状态只能填写：在职、请假或离职");
        return null;
    }

    private Date parseBirthday(String value, List<String> errors) {
        value = normalize(value);
        if (value == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        format.setLenient(false);
        try {
            Date result = format.parse(value);
            if (!value.equals(format.format(result))) {
                throw new ParseException(value, 0);
            }
            return result;
        } catch (ParseException e) {
            errors.add("生日必须使用 yyyy-MM-dd 格式");
            return null;
        }
    }

    private void required(String value, String label, List<String> errors) {
        if (value == null) {
            errors.add(label + "不能为空");
        }
    }

    private boolean isEmptyImportRow(UserImportRowVo row) {
        return StrUtil.isAllBlank(row.getId(), row.getUsername(), row.getName(), row.getDepartment(),
                row.getRoleNames(), row.getTel(), row.getEmail(), row.getSex(), row.getWorkStatus(),
                row.getStatus(), row.getAdminEnabled(), row.getBirthday(), row.getAddress(),
                row.getDescription(), row.getWxUnionId(), row.getWxMaOpenid(), row.getPassword());
    }

    private String normalize(String value) {
        return StrUtil.trimToNull(value);
    }

    private String normalizePath(String value) {
        String[] parts = StrUtil.trimToEmpty(value).replace('／', '/').split("/");
        return java.util.Arrays.stream(parts)
                .map(StrUtil::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.joining(USER_IMPORT_DEPARTMENT_SEPARATOR));
    }

    private static class ImportScope {
        private final Set<String> visibleUserIds;
        private final Set<String> departmentIds;

        private ImportScope(Set<String> visibleUserIds, Set<String> departmentIds) {
            this.visibleUserIds = visibleUserIds;
            this.departmentIds = departmentIds;
        }

        private boolean allowsUser(User user) {
            return user != null
                    && (visibleUserIds == null || visibleUserIds.contains(user.getId()))
                    && allowsDepartment(user.getDepartmentId());
        }

        private boolean allowsDepartment(String departmentId) {
            return departmentIds == null || departmentIds.contains(departmentId);
        }
    }

    private static class DepartmentIndex {
        private final Map<String, List<Department>> byPath = new LinkedHashMap<>();
        private final Map<String, List<Department>> byName = new LinkedHashMap<>();
    }

    private static class UserImportPlan {
        private final int totalCount;
        private final List<UserImportChange> changes = new ArrayList<>();
        private final List<UserImportErrorVo> errors = new ArrayList<>();
        private int createCount;
        private int updateCount;

        private UserImportPlan(int totalCount) {
            this.totalCount = totalCount;
        }

        private UserImportPreviewVo toPreview() {
            UserImportPreviewVo preview = new UserImportPreviewVo();
            preview.setTotalCount(totalCount);
            preview.setValidCount(changes.size());
            preview.setErrorCount(errors.size());
            preview.setCreateCount(createCount);
            preview.setUpdateCount(updateCount);
            preview.setErrors(errors);
            return preview;
        }
    }

    private static class UserImportChange {
        private final User entity;
        private final boolean update;

        private UserImportChange(User entity, boolean update) {
            this.entity = entity;
            this.update = update;
        }
    }

    public TableRet<User> selectSuperPageByQuery(QueryParams query) {
        return super.selectPageByQuery(query);
    }

    @Override
    public QueryWrapper<User> parseQuery(QueryParams query) {
        QueryWrapper<User> wrapper = super.parseQuery(query);
        if (!isSuperAdminUser(getCurrentUserId())) {
            wrapper.ne("id", CommonConstants.SUPER_ADMIN_ID);
        }
        return wrapper;
    }

    private void appendTenantUserQueryIfNeed(QueryParams query) {
        if (!faSetting.isTenantEnabled()) {
            return;
        }
        String tenantId = TenantContext.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            if (isSuperAdminUser(getCurrentUserId())) {
                return;
            }
            tenantId = TenantContext.requireTenantId();
        }

        List<String> tenantUserIds = tenantUserBiz.getUserIdsByTenantId(tenantId);
        if (query.getQuery() == null) {
            query.setQuery(new HashMap<>());
        }

        Object existIdIn = query.getQuery().get("id#$in");
        if (existIdIn instanceof List<?> existList) {
            List<String> intersectIds = existList.stream()
                    .map(ObjectUtil::toString)
                    .filter(tenantUserIds::contains)
                    .collect(Collectors.toList());
            query.getQuery().put("id#$in", CollUtil.isEmpty(intersectIds) ? List.of("__tenant_no_user__") : intersectIds);
            return;
        }

        query.getQuery().put("id#$in", CollUtil.isEmpty(tenantUserIds) ? List.of("__tenant_no_user__") : tenantUserIds);
    }

    /**
     * 新增、编辑时，校验数据合理性
     *
     * @param entity
     */
    private void checkBeanValid(User entity) {
        // 插入时校验手机号是否重复
        long telCount = lambdaQuery()
                .eq(User::getTel, entity.getTel())
                .ne(entity.getId() != null, User::getId, entity.getId())
                .count();
        if (telCount > 0) throw new BuzzException("手机号重复");

        // 校验用户名是否重复
        long usernameCount = lambdaQuery()
                .eq(User::getUsername, entity.getUsername())
                .ne(entity.getId() != null, User::getId, entity.getId())
                .count();
        if (usernameCount > 0) throw new BuzzException("账户重复");
    }

    /**
     * 更新用户角色
     *
     * @param entity
     */
    public void updateUserRoles(User entity) {
        // 关联角色
        rbacUserRoleBiz.changeUserRoles(entity.getId(), entity.getRoleIds());
        // 冗余角色名称
        List<RbacRole> roleList = rbacUserRoleBiz.getUserRoles(entity.getId());
        String roleNames = IterUtil.join(roleList.stream().map(RbacRole::getName).iterator(), ",");

        lambdaUpdate().eq(User::getId, entity.getId()).set(User::getRoleNames, roleNames).update();
        entity.setRoleNames(roleNames);
    }

//    @Cached(name = "user:", key = "#id")
    @Override
    public User getById(Serializable id) {
        return super.getById(id);
    }

    @Override
    public boolean save(User entity) {
        this.checkBeanValid(entity);

        // 密码加密存储
        String password = FaPwdUtils.encryptPwd(entity.getPassword());
        entity.setPassword(password);
        if (entity.getAdminEnabled() == null) {
            entity.setAdminEnabled(false);
        }

        super.save(entity);

        this.bindTenantUserIfNeed(entity);
        this.updateUserRoles(entity);

        return true;
    }

    private void bindTenantUserIfNeed(User entity) {
        if (!faSetting.isTenantEnabled()) {
            return;
        }

        Department department = departmentBiz.getById(entity.getDepartmentId());
        String tenantId = department == null ? null : department.getTenantId();
        if (StrUtil.isBlank(tenantId)) {
            tenantId = getCurrentTenantId();
        }
        if (StrUtil.isBlank(tenantId)) {
            throw new BuzzException("新增用户所属部门租户为空");
        }

        tenantUserBiz.bindUserTenantIfAbsent(tenantId, entity.getId());
    }

//    @CacheInvalidate(name = "user:", key = "#entity.id")
    @FaCacheClear(pre = "rbac:userMenus:", key = "id")
    @Override
    public boolean updateById(User entity) {
        User beanDB = getById(entity.getId());
        if (beanDB == null) throw new NoDataException();

        ensureSuperAdminStatus(entity);
        this.checkBeanValid(entity);

        // 修改用户，不能修改用户密码
        entity.setPassword(beanDB.getPassword());

        this.updateUserRoles(entity);

        return super.updateById(entity);
    }

    @Override
    public boolean updateBatchById(Collection<User> entityList) {
        if (entityList != null) {
            entityList.forEach(this::ensureSuperAdminStatus);
        }
        return super.updateBatchById(entityList);
    }

    private void ensureSuperAdminStatus(User entity) {
        if (!CommonConstants.SUPER_ADMIN_ID.equals(entity.getId())) {
            return;
        }
        if (Boolean.FALSE.equals(entity.getStatus())) {
            throw new BuzzException("超级管理员账户必须保持有效");
        }
        entity.setStatus(true);
    }

    /**
     * 限定一些属性的简单更新
     * @param entity
     * @return
     */
//    @CacheInvalidate(name = "user:", key = "#entity.id")
    @FaCacheClear(pre = "rbac:userMenus:", key = "id")
    public boolean updateSimpleById(User entity) {
        // 不可以将自己的账户状态修改为false
        if (CommonConstants.SUPER_ADMIN_ID.equals(entity.getId())) {
            throw new BuzzException("不能修改超级管理员账户状态");
        }

        if (getCurrentUserId().equals(entity.getId())) {
            throw new BuzzException("不能修改自己的账户状态");
        }

        // 可以更新的属性
        return lambdaUpdate()
                .set(entity.getStatus() != null, User::getStatus, entity.getStatus())
                .set(entity.getAdminEnabled() != null, User::getAdminEnabled, entity.getAdminEnabled())
                .eq(User::getId, entity.getId())
                .update();
    }

//    @CacheInvalidate(name = "user:", key = "#id")
    @Override
    public boolean removeById(Serializable id) {
        // 不能删除自身账户和admin账户
        if (ObjectUtil.equal(id, "1") || ObjectUtil.equal(id, getCurrentUserId())) {
            throw new BuzzException("不能删除自身账户");
        }

        return super.removeById(id);
    }


    /**
     * 根据用户名获取用户信息 FIX-ME: 用户名不一定唯一，感觉换成ID会更好
     *
     * @param username 用户名
     */
    public User getUserByUsername(String username) {
        return lambdaQuery().eq(User::getUsername, username).one();
    }

    public User getUserByTel(String tel) {
        return lambdaQuery().eq(User::getTel, tel).one();
    }

    public User findByApiToken(String token) {
        if (StringUtils.isEmpty(token)) throw new BuzzException("token为空");

        return lambdaQuery().eq(User::getApiToken, token).one();
    }

    @Override
    protected void preProcessQuery(QueryParams query) {
        // 部门分组级联查询
        Map<String, Object> queryMap = query.getQuery();
        if (queryMap.containsKey("departmentIdSuper")) {
            String departmentId = MapUtils.getString(queryMap, "departmentIdSuper");
            queryMap.remove("departmentIdSuper");

            List<Department> departmentList = departmentBiz.findAllChildren(departmentId);
            if (departmentList != null && !departmentList.isEmpty()) {
                List<String> departmentIdList = departmentList.stream().map(Department::getId).collect(Collectors.toList());
                queryMap.put("departmentId#$in", departmentIdList);
                queryMap.remove("departmentId");
            }
        }
    }

    @Override
    public void decorateOne(User i) {
        i.setSuperAdmin(isSuperAdminUser(i.getId()));
        Department department = departmentBiz.getByIdWithCache(i.getDepartmentId());
        if (department != null) {
            i.setDepartmentName(department.getName());
        }
    }

    public boolean resetPwd(Map<String, Object> params) {
        Integer id = (Integer) params.get("id");
        String newPwd = (String) params.get("newPwd");

        User beanDB = getById(id);
        if (beanDB == null) throw new NoDataException();

        String password = FaPwdUtils.encryptPwd(newPwd);
        beanDB.setPassword(password);
        return super.updateById(beanDB);
    }

//    @CacheInvalidate(name = "user:", key = "#userId")
    public boolean updateMine(String userId, UserAccountVo vo) {
        // 插入时校验手机号是否重复
        long telCount = lambdaQuery()
                .eq(User::getTel, vo.getTel())
                .ne(User::getId, userId)
                .count();
        if (telCount > 0) throw new BuzzException("手机号重复");

        // 校验用户名是否重复
        long usernameCount = lambdaQuery()
                .eq(User::getUsername, vo.getUsername())
                .ne(User::getId, userId)
                .count();
        if (usernameCount > 0) throw new BuzzException("账户重复");

        User user = getById(userId);
        // 手动处理 sex 字段的转换
        if (vo.getSex() != null) {
            SexEnum sexEnum = SexEnum.fromValue(vo.getSex());
            user.setSex(sexEnum);
        }
        BeanUtils.copyProperties(vo, user,"sex");
        return super.updateById(user);
    }

//    @CacheInvalidate(name = "user:", key = "#userId")
    public boolean updateMyPwd(String userId, Map<String, Object> params) {
        String oldPwd = (String) params.get("oldPwd");
        String newPwd = (String) params.get("newPwd");

        if (oldPwd.equals(newPwd)) throw new BuzzException("新旧密码不能一样");

        User user = getById(userId);

        this.validateCurrentUserPwd(oldPwd);

        String password = FaPwdUtils.encryptPwd(newPwd);
        user.setPassword(password);
        return super.updateById(user);
    }

//    @CacheInvalidate(name = "user:", key = "#userId")
    public boolean updateMyApiToken(String userId) {
        User user = getById(userId);
        user.setApiToken(UUID.fastUUID().toString(true));
        return super.updateById(user);
    }

    @Transactional
    public void updateBatchDept(UserBatchUpdateDeptVo params) {
        lambdaUpdate()
                .set(User::getDepartmentId, params.getDepartmentId())
                .in(User::getId, params.getUserIds())
                .update();

        delUserCacheByIds(params.getUserIds());
    }

    @Transactional
    public void updateBatchRole(UserBatchUpdateRoleVo params) {
        params.getUserIds().forEach(userId -> {
            rbacUserRoleBiz.changeUserRoles(userId, params.getRoleIds());
        });

        delUserCacheByIds(params.getUserIds());
    }

    public void updateBatchPwd(UserBatchUpdatePwdVo params) {
        this.validateCurrentUserPwd(params.getPasswordCheck());

        String newPwd = params.getNewPwd();
        if (StringUtils.isEmpty(newPwd)) {
            throw new BuzzException("新密码不能为空");
        }

        // TODO 根据系统配置验证密码合法性
//        if (newPwd.trim().length() < 6 || newPwd.trim().length() > 32) {
//            throw new BuzzException("新密码长度错误");
//        }

        String password = FaPwdUtils.encryptPwd(newPwd.trim());
        params.getUserIds().forEach(id -> {
            User user = getById(id);
            user.setPassword(password);
            super.updateById(user);
        });
    }

    public void delUserCacheByIds(List<String> userIds) {
        userIds.forEach(i -> {
            delUserCacheById(i);
        });
    }

    public void delUserCacheById(String userId) {
        redisson.getKeys().deleteByPattern(redisPrefix + ":user:" + userId);
    }

    public void accountAdminDelete(Map<String, Object> params) {
        String passwordCheck = (String) params.get("passwordCheck");
        List<Integer> ids = (List<Integer>) params.get("ids");

        this.validateCurrentUserPwd(passwordCheck);

        ids.forEach(id -> {
            removeById(id);
        });
    }

    public User getUserFromApiToken() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = request.getHeader(faSetting.getApi().getTokenApiHeader());
        if (StrUtil.isEmpty(token)) throw new BuzzException("Token Is Empty");
        UserToken userToken = userTokenBiz.getById(token);
        if (userToken == null || !userToken.getValid()) throw new BuzzException("Token Not Valid");

        return super.getById(userToken.getUserId());
    }

    public Long jumpCount(UserJumpCountVo query) {
        if (ObjectUtil.isAllEmpty(query.getUsername(), query.getTel())) return 0L;
        return lambdaQuery()
                .eq(query.getUsername() != null, User::getUsername, query.getUsername())
                .eq(query.getTel() != null, User::getTel, query.getTel())
                .count();
    }

    public void registry(UserRegistryVo params) {
        if (!ObjectUtil.equal(params.getPassword(), params.getPasswordConfirm())) {
            throw new BuzzException("两次输入的密码不一致");
        }

        User entity = new User();
        BeanUtils.copyProperties(params, entity);

        this.checkBeanValid(entity);

        // 密码加密
        String password = FaPwdUtils.encryptPwd(params.getPassword());
        entity.setPassword(password);

        entity.setStatus(true);
        entity.setAdminEnabled(false);
        entity.setDepartmentId("1");

        super.save(entity);

        // 设置初始角色
        RbacRole rbacRole = rbacRoleBiz.getRoleByName(CommonConstants.REGISTRY_USER_ROLE_NAME);
        entity.setRoleId(rbacRole.getId());

        this.updateUserRoles(entity);
    }

    public void forgetResetPwd(UserForgetResetPwdVo params) {
        LambdaQueryChainWrapper<User> chainWrapper = lambdaQuery()
                .eq(User::getUsername, params.getUsername())
                .eq(User::getTel, params.getTel());
        long count = chainWrapper.count();
        if (count != 1) throw new BuzzException("未找到匹配账户，请确认账户、手机号");

        User user = chainWrapper.one();

        String password = FaPwdUtils.encryptPwd(params.getPassword());
        lambdaUpdate()
                .set(User::getPassword, password)
                .eq(User::getId, user.getId())
                .update();
    }

    public void setUserLogin(String userId) {
        setUserLogin(userId, "web");
    }

    public void setUserLogin(User user) {
        setUserLogin(user, "web");
    }

    public void setUserLogin(String userId, String device) {
        User user = super.getById(userId);
        this.setUserLogin(user, device);
    }

    public void setUserLogin(User user, String device) {
        UserCheckUtil.checkUserValid(user);

        BaseContextHandler.setUsername(user.getUsername());
        BaseContextHandler.setName(user.getName());
        BaseContextHandler.setUserId(user.getId());
        BaseContextHandler.setLogin(true);

        // 更新用户最后在线时间
        this.lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getLastOnlineTime, new Date())
                .update();
    }

    /**
     * 通过姓名获取用户（带缓存）（重复姓名取top）
     * 注意：这里用户数量较多的话，可能会导致用户名重复的问题，自行斟酌使用
     * @param name {@link User#getName()}
     * @return {@link User}
     */
    public User getByNameWithCache(String name) {
        Map<Serializable, User> cache = BaseContextHandler.getCacheMap("UserBiz.getByNameWithCache");
        if (cache.containsKey(name)) {
            return cache.get(name);
        }

        User entity = getTop(
                lambdaQuery()
                        .eq(User::getName, name)
                        .orderByDesc(User::getId)
        );
        cache.put(name, entity);
        return entity;
    }

    @Override
    public User getDetailById(Serializable id) {
        // 调用父类的方法获取基本数据
        User user = super.getById(id);
        this.populateUserRoles(user);
        this.decorateOne(user);
        return user;
    }
    /**
     * 根据用户的ID查找角色信息
     *
     * @param user
     */
    private void populateUserRoles(User user) {
        if (user != null && user.getId() != null) {
            // 查找与当前用户相关的角色数据
            List<Long> userRoles = rbacUserRoleBiz.getUserRoleIds(user.getId());
            // 处理角色信息，假设返回的是一个角色列表
            if (userRoles != null && !userRoles.isEmpty()) {
                user.setRoleIds(userRoles);
            }
        }
    }
}
