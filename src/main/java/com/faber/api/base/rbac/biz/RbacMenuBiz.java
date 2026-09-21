package com.faber.api.base.rbac.biz;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.api.base.admin.biz.EntityLogBiz;
import com.faber.api.base.rbac.entity.RbacMenu;
//import com.alicp.jetcache.anno.Cached;
import com.faber.api.base.rbac.enums.RbacMenuLevelEnum;
import com.faber.api.base.rbac.enums.RbacMenuScopeEnum;
import com.faber.api.base.rbac.mapper.RbacMenuMapper;
import com.faber.api.base.rbac.vo.query.RbacMenuExportReqVo;
import com.faber.api.base.rbac.vo.query.RbacMenuImportReqVo;
import com.faber.api.base.rbac.vo.ret.RbacMenuExportVo;
import com.faber.api.base.rbac.vo.ret.RbacMenuImportPreviewVo;
import com.faber.api.base.rbac.vo.ret.RbacMenuImportResultVo;
import com.faber.api.base.tn.biz.TenantPermissionBiz;
import com.faber.core.config.redis.annotation.FaCacheClear;
import com.faber.core.exception.BuzzException;
import com.faber.core.service.FaFlowService;
import com.faber.core.utils.FaFileUtils;
import com.faber.core.utils.FaHttpUtils;
import com.faber.core.vo.tree.TreeNode;
import com.faber.core.vo.tree.TreePosChangeVo;
import com.faber.core.vo.utils.FaOption;
import com.faber.core.web.biz.BaseTreeBiz;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * BASE-权限表
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-19 11:40:40
 */
@Service
public class RbacMenuBiz extends BaseTreeBiz<RbacMenuMapper, RbacMenu> {

    private static final int CONFIG_KEY_MAX_LENGTH = 64;
    private static final String EXPORT_SCHEMA = "fa-admin.menu";
    private static final int EXPORT_VERSION = 1;

    @Resource FaFlowService faFlowService;
    @Resource ObjectMapper objectMapper;
    @Resource EntityLogBiz entityLogBiz;
    @Lazy
    @Resource TenantPermissionBiz tenantPermissionBiz;

//    @Cached(name="rbac:allMenuTree", key="new String('')", expire = 3600)
    @Override
    public List<TreeNode<RbacMenu>> allTree() {
        return super.allTree();
    }

    @EventListener(ApplicationReadyEvent.class)
    @FaCacheClear(pre = "rbac:")
    @Transactional(rollbackFor = Exception.class)
    public void initLegacyConfigKey() {
        List<RbacMenu> menus = lambdaQuery().list().stream()
                .filter(menu -> StrUtil.isBlank(menu.getConfigKey()))
                .toList();
        for (RbacMenu menu : menus) {
            RbacMenu update = new RbacMenu();
            update.setId(menu.getId());
            update.setConfigKey(generateLegacyConfigKey(menu));
            if (!super.updateById(update)) {
                throw new BuzzException("菜单配置标识初始化失败: " + menu.getId());
            }
        }
        if (!menus.isEmpty()) {
            _logger.info("菜单配置标识初始化完成，补全数量：{}", menus.size());
        }
    }

    public void exportJson(RbacMenuExportReqVo request) throws IOException {
        if (request == null || request.getScope() == null) {
            throw new BuzzException("导出范围不能为空");
        }

        RbacMenuScopeEnum scope = request.getScope();
        List<RbacMenu> menus = lambdaQuery()
                .eq(RbacMenu::getScope, scope)
                .orderByAsc(RbacMenu::getParentId)
                .orderByAsc(RbacMenu::getSort)
                .orderByAsc(RbacMenu::getId)
                .list();
        Map<Long, String> configKeyMap = new HashMap<>(menus.size());
        for (RbacMenu menu : menus) {
            if (StrUtil.isBlank(menu.getConfigKey())) {
                throw new BuzzException("菜单配置标识未初始化，请先执行菜单配置标识迁移");
            }
            configKeyMap.put(menu.getId(), menu.getConfigKey());
        }

        List<RbacMenuExportVo.Node> nodes = new ArrayList<>(menus.size());
        for (RbacMenu menu : menus) {
            String parentConfigKey = null;
            if (menu.getParentId() != null && menu.getParentId() != 0L) {
                parentConfigKey = configKeyMap.get(menu.getParentId());
                if (StrUtil.isBlank(parentConfigKey)) {
                    throw new BuzzException("菜单父节点不存在，无法导出: " + menu.getId());
                }
            }
            nodes.add(RbacMenuExportVo.Node.from(menu, parentConfigKey));
        }

        RbacMenuExportVo export = new RbacMenuExportVo();
        export.setSchema(EXPORT_SCHEMA);
        export.setVersion(EXPORT_VERSION);
        export.setScope(scope);
        export.setExportedAt(java.time.Instant.now().toString());
        export.setNodes(nodes);

        FaHttpUtils.getHttpServletResponse().setContentType("application/json;charset=UTF-8");
        String filename = "menu-" + scope.getDesc() + "-v" + EXPORT_VERSION + ".json";
        FaFileUtils.download(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(export), filename);
    }

    public RbacMenuImportPreviewVo previewImport(RbacMenuImportReqVo request) {
        return buildImportPlan(request).toPreview();
    }

    @FaCacheClear(pre = "rbac:")
    @Transactional(rollbackFor = Exception.class)
    public RbacMenuImportResultVo commitImport(RbacMenuImportReqVo request) {
        MenuImportPlan plan = buildImportPlan(request);
        if (plan.conflictCount > 0) {
            throw new BuzzException("菜单导入存在冲突，请修正 JSON 后重新预览");
        }
        validateImportTenantRequired(plan);
        Map<String, Long> idByConfigKey = new HashMap<>();
        plan.targetByConfigKey.forEach((configKey, menu) -> idByConfigKey.put(configKey, menu.getId()));
        int createdCount = 0;
        int updatedCount = 0;
        for (RbacMenuExportVo.Node node : plan.orderedNodes) {
            Long parentId = StrUtil.isBlank(node.getParentConfigKey())
                    ? 0L
                    : idByConfigKey.get(node.getParentConfigKey());
            if (parentId == null) {
                throw new BuzzException("菜单父节点未找到: " + node.getParentConfigKey());
            }

            RbacMenu entity = new RbacMenu();
            entity.setConfigKey(node.getConfigKey());
            entity.setParentId(parentId);
            entity.setName(node.getName());
            entity.setSort(node.getSort());
            entity.setScope(plan.scope);
            entity.setTenantRequired(Boolean.TRUE.equals(node.getTenantRequired()));
            entity.setLevel(node.getLevel());
            entity.setIcon(emptyIfNull(node.getIcon()));
            entity.setStatus(node.getStatus());
            entity.setLinkType(node.getLinkType());
            entity.setLinkUrl(emptyIfNull(node.getLinkUrl()));

            RbacMenu current = plan.targetByConfigKey.get(node.getConfigKey());
            if (current == null) {
                if (baseMapper.insert(entity) <= 0 || entity.getId() == null) {
                    throw new BuzzException("菜单导入失败: " + node.getName());
                }
                idByConfigKey.put(node.getConfigKey(), entity.getId());
                if (Boolean.TRUE.equals(entity.getTenantRequired())) {
                    syncTenantRequiredPermission(entity, false, true);
                }
                createdCount++;
            } else {
                entity.setId(current.getId());
                boolean requiredChanged = !Objects.equals(
                        Boolean.TRUE.equals(current.getTenantRequired()),
                        Boolean.TRUE.equals(entity.getTenantRequired())
                );
                if (baseMapper.updateById(entity) <= 0) {
                    throw new BuzzException("菜单更新失败: " + node.getName());
                }
                if (requiredChanged) {
                    syncTenantRequiredPermission(
                            current,
                            Boolean.TRUE.equals(current.getTenantRequired()),
                            Boolean.TRUE.equals(entity.getTenantRequired())
                    );
                }
                if (plan.changedConfigKeys.contains(node.getConfigKey())) {
                    updatedCount++;
                }
            }
        }

        RbacMenuImportResultVo result = new RbacMenuImportResultVo();
        result.setCreatedCount(createdCount);
        result.setUpdatedCount(updatedCount);
        result.setUnchangedCount(plan.unchangedCount);
        return result;
    }

    private void validateImportTenantRequired(MenuImportPlan plan) {
        boolean permissionChanged = plan.orderedNodes.stream().anyMatch(node -> {
            RbacMenu current = plan.targetByConfigKey.get(node.getConfigKey());
            return !Objects.equals(
                    current == null ? false : Boolean.TRUE.equals(current.getTenantRequired()),
                    Boolean.TRUE.equals(node.getTenantRequired())
            );
        });
        if (permissionChanged) {
            checkTenantRequiredPermission();
        }
    }

    private MenuImportPlan buildImportPlan(RbacMenuImportReqVo request) {
        if (request == null || request.getScope() == null || request.getConfig() == null) {
            throw new BuzzException("导入范围和 JSON 配置不能为空");
        }
        RbacMenuExportVo config = request.getConfig();
        if (!EXPORT_SCHEMA.equals(config.getSchema())) {
            throw new BuzzException("不支持的菜单 JSON schema");
        }
        if (!Objects.equals(EXPORT_VERSION, config.getVersion())) {
            throw new BuzzException("不支持的菜单 JSON 版本: " + config.getVersion());
        }
        if (config.getScope() == null || config.getScope() != request.getScope()) {
            throw new BuzzException("导入文件 scope 与当前目标 scope 不一致");
        }

        List<RbacMenuExportVo.Node> nodes = config.getNodes() == null ? List.of() : config.getNodes();
        if (nodes.size() > 5000) {
            throw new BuzzException("单次最多导入 5000 个菜单节点");
        }

        Map<String, RbacMenuExportVo.Node> sourceByConfigKey = new LinkedHashMap<>();
        Set<Long> sourceIds = new HashSet<>();
        for (RbacMenuExportVo.Node node : nodes) {
            if (node == null) {
                throw new BuzzException("菜单 JSON 中存在空节点");
            }
            String configKey = trimRequired(node.getConfigKey(), "configKey");
            node.setConfigKey(configKey);
            if (configKey.length() > CONFIG_KEY_MAX_LENGTH) {
                throw new BuzzException("菜单配置标识长度不能超过" + CONFIG_KEY_MAX_LENGTH + "个字符: " + configKey);
            }
            if (sourceByConfigKey.put(configKey, node) != null) {
                throw new BuzzException("菜单 JSON 存在重复 configKey: " + configKey);
            }
            if (node.getId() != null && !sourceIds.add(node.getId())) {
                throw new BuzzException("菜单 JSON 存在重复菜单 ID: " + node.getId());
            }
            node.setParentConfigKey(trimOptional(node.getParentConfigKey()));
            node.setName(trimRequired(node.getName(), "菜单名称"));
            if (node.getName().length() > 255) {
                throw new BuzzException("菜单名称长度不能超过 255 个字符: " + node.getName());
            }
            if (node.getSort() == null || node.getLevel() == null || node.getStatus() == null || node.getLinkType() == null) {
                throw new BuzzException("菜单 JSON 缺少排序、等级、状态或链接类型: " + configKey);
            }
            if (node.getIcon() != null && node.getIcon().length() > 255) {
                throw new BuzzException("菜单图标标识长度不能超过 255 个字符: " + configKey);
            }
            if (node.getLinkUrl() != null && node.getLinkUrl().length() > 255) {
                throw new BuzzException("菜单链接地址长度不能超过 255 个字符: " + configKey);
            }
            node.setIcon(emptyIfNull(node.getIcon()));
            node.setLinkUrl(emptyIfNull(node.getLinkUrl()));
            node.setTenantRequired(Boolean.TRUE.equals(node.getTenantRequired()));
        }
        validateImportTree(sourceByConfigKey);

        List<RbacMenu> targetMenus = lambdaQuery()
                .eq(RbacMenu::getScope, request.getScope())
                .list();
        Map<String, RbacMenu> targetByConfigKey = new LinkedHashMap<>();
        Map<Long, String> targetConfigKeyById = new HashMap<>();
        for (RbacMenu menu : targetMenus) {
            if (StrUtil.isNotBlank(menu.getConfigKey())) {
                targetByConfigKey.put(menu.getConfigKey(), menu);
                targetConfigKeyById.put(menu.getId(), menu.getConfigKey());
            }
        }

        Map<String, RbacMenu> ownerByConfigKey = new HashMap<>();
        if (!sourceByConfigKey.isEmpty()) {
            lambdaQuery().in(RbacMenu::getConfigKey, sourceByConfigKey.keySet()).list()
                    .forEach(menu -> ownerByConfigKey.put(menu.getConfigKey(), menu));
        }

        Map<String, Integer> depths = new HashMap<>();
        for (String configKey : sourceByConfigKey.keySet()) {
            getImportDepth(configKey, sourceByConfigKey, depths);
        }
        List<RbacMenuExportVo.Node> orderedNodes = new ArrayList<>(nodes);
        orderedNodes.sort(Comparator.comparingInt(node -> depths.get(node.getConfigKey())));

        MenuImportPlan plan = new MenuImportPlan(request.getScope(), orderedNodes, targetByConfigKey);
        for (RbacMenuExportVo.Node node : nodes) {
            RbacMenu owner = ownerByConfigKey.get(node.getConfigKey());
            if (owner != null && owner.getScope() != request.getScope()) {
                plan.add("CONFLICT", node.getConfigKey(), node.getName(), "configKey 已被其他 scope 菜单占用");
                continue;
            }

            RbacMenu current = targetByConfigKey.get(node.getConfigKey());
            if (current == null) {
                plan.add("CREATE", node.getConfigKey(), node.getName(), "目标环境新增");
                continue;
            }

            if (current.getLevel() != node.getLevel()) {
                plan.add("CONFLICT", node.getConfigKey(), node.getName(), "菜单等级发生变化，需人工处理");
                continue;
            }
            String currentParentKey = getParentConfigKey(current, targetConfigKeyById);
            boolean moved = !Objects.equals(currentParentKey, node.getParentConfigKey());
            boolean sorted = !Objects.equals(current.getSort(), node.getSort());
            boolean updated = !Objects.equals(current.getName(), node.getName())
                    || !Objects.equals(emptyIfNull(current.getIcon()), node.getIcon())
                    || !Objects.equals(current.getStatus(), node.getStatus())
                    || !Objects.equals(current.getTenantRequired(), node.getTenantRequired())
                    || !Objects.equals(current.getLinkType(), node.getLinkType())
                    || !Objects.equals(emptyIfNull(current.getLinkUrl()), node.getLinkUrl());
            if (!moved && !sorted && !updated) {
                plan.add("UNCHANGED", node.getConfigKey(), node.getName(), "目标环境已是最新");
                continue;
            }
            plan.changedConfigKeys.add(node.getConfigKey());
            if (moved) plan.add("MOVE", node.getConfigKey(), node.getName(), "调整父级菜单");
            if (sorted) plan.add("SORT", node.getConfigKey(), node.getName(), "调整排序");
            if (updated) plan.add("UPDATE", node.getConfigKey(), node.getName(), "覆盖菜单属性");
            plan.updatedCount++;
        }
        for (RbacMenu menu : targetMenus) {
            if (!sourceByConfigKey.containsKey(menu.getConfigKey())) {
                plan.add("EXTRA", menu.getConfigKey(), menu.getName(), "目标环境保留，不会删除");
            }
        }
        return plan;
    }

    private void validateImportTree(Map<String, RbacMenuExportVo.Node> sourceByConfigKey) {
        for (RbacMenuExportVo.Node node : sourceByConfigKey.values()) {
            String parentConfigKey = node.getParentConfigKey();
            if (StrUtil.isBlank(parentConfigKey)) {
                if (node.getLevel() != RbacMenuLevelEnum.APP) {
                    throw new BuzzException("根节点必须是模块: " + node.getConfigKey());
                }
                continue;
            }
            RbacMenuExportVo.Node parent = sourceByConfigKey.get(parentConfigKey);
            if (parent == null) {
                throw new BuzzException("菜单父节点不存在: " + parentConfigKey);
            }
            if (parent.getLevel() == RbacMenuLevelEnum.APP && node.getLevel() != RbacMenuLevelEnum.MENU) {
                throw new BuzzException("模块下只能挂载菜单: " + node.getConfigKey());
            }
            if (parent.getLevel() == RbacMenuLevelEnum.MENU
                    && node.getLevel() != RbacMenuLevelEnum.MENU
                    && node.getLevel() != RbacMenuLevelEnum.BUTTON) {
                throw new BuzzException("菜单下只能挂载菜单或按钮: " + node.getConfigKey());
            }
            if (parent.getLevel() == RbacMenuLevelEnum.BUTTON) {
                throw new BuzzException("按钮不能作为父节点: " + parentConfigKey);
            }
        }
        for (String configKey : sourceByConfigKey.keySet()) {
            Set<String> path = new HashSet<>();
            String current = configKey;
            while (StrUtil.isNotBlank(current)) {
                if (!path.add(current)) {
                    throw new BuzzException("菜单 JSON 存在父子循环: " + configKey);
                }
                current = sourceByConfigKey.get(current).getParentConfigKey();
            }
        }
    }

    private int getImportDepth(
            String configKey,
            Map<String, RbacMenuExportVo.Node> sourceByConfigKey,
            Map<String, Integer> depths
    ) {
        Integer cached = depths.get(configKey);
        if (cached != null) return cached;
        int depth = 0;
        String current = configKey;
        while (StrUtil.isNotBlank(current)) {
            Integer currentDepth = depths.get(current);
            if (currentDepth != null) {
                depth += currentDepth;
                break;
            }
            String parentConfigKey = sourceByConfigKey.get(current).getParentConfigKey();
            if (StrUtil.isBlank(parentConfigKey)) break;
            depth++;
            current = parentConfigKey;
        }
        depths.put(configKey, depth);
        return depth;
    }

    private String getParentConfigKey(RbacMenu menu, Map<Long, String> configKeyById) {
        if (menu.getParentId() == null || menu.getParentId() == 0L) return null;
        return configKeyById.get(menu.getParentId());
    }

    private String trimRequired(String value, String fieldName) {
        String result = StrUtil.trim(value);
        if (StrUtil.isBlank(result)) {
            throw new BuzzException(fieldName + "不能为空");
        }
        return result;
    }

    private String trimOptional(String value) {
        String result = StrUtil.trim(value);
        return StrUtil.isBlank(result) ? null : result;
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    private static final class MenuImportPlan {
        private final RbacMenuScopeEnum scope;
        private final List<RbacMenuExportVo.Node> orderedNodes;
        private final Map<String, RbacMenu> targetByConfigKey;
        private final List<RbacMenuImportPreviewVo.Change> changes = new ArrayList<>();
        private final Set<String> changedConfigKeys = new HashSet<>();
        private int createCount;
        private int updatedCount;
        private int moveCount;
        private int sortCount;
        private int unchangedCount;
        private int conflictCount;
        private int extraCount;

        private MenuImportPlan(
                RbacMenuScopeEnum scope,
                List<RbacMenuExportVo.Node> orderedNodes,
                Map<String, RbacMenu> targetByConfigKey
        ) {
            this.scope = scope;
            this.orderedNodes = orderedNodes;
            this.targetByConfigKey = targetByConfigKey;
        }

        private void add(String type, String configKey, String name, String detail) {
            changes.add(new RbacMenuImportPreviewVo.Change(type, configKey, name, detail));
            switch (type) {
                case "CREATE" -> createCount++;
                case "MOVE" -> moveCount++;
                case "SORT" -> sortCount++;
                case "UNCHANGED" -> unchangedCount++;
                case "CONFLICT" -> conflictCount++;
                case "EXTRA" -> extraCount++;
                default -> {
                }
            }
        }

        private RbacMenuImportPreviewVo toPreview() {
            RbacMenuImportPreviewVo result = new RbacMenuImportPreviewVo();
            result.setScope(scope);
            result.setTotal(orderedNodes.size());
            result.setCreateCount(createCount);
            result.setUpdateCount(updatedCount);
            result.setMoveCount(moveCount);
            result.setSortCount(sortCount);
            result.setUnchangedCount(unchangedCount);
            result.setConflictCount(conflictCount);
            result.setExtraCount(extraCount);
            result.setChanges(changes);
            return result;
        }
    }

    @FaCacheClear(pre = "rbac:")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean save(RbacMenu entity) {
        if (Boolean.TRUE.equals(entity.getTenantRequired())) {
            checkTenantRequiredPermission();
        }
        if (StrUtil.isBlank(entity.getConfigKey())) {
            entity.setConfigKey(generateConfigKey());
        } else {
            entity.setConfigKey(entity.getConfigKey().trim());
        }
        validateConfigKey(entity.getConfigKey(), entity.getId());
//        long count = lambdaQuery().eq(RbacMenu::getLinkUrl, entity.getLinkUrl()).count();
//        if (count > 0) throw new BuzzException("链接已存在，不可重复录入");

        boolean saved = super.save(entity);
        if (saved && Boolean.TRUE.equals(entity.getTenantRequired()) && entity.getId() != null) {
            syncTenantRequiredPermission(entity, false, true);
        }
        return saved;
    }

    @FaCacheClear(pre = "rbac:")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateById(RbacMenu entity) {
        RbacMenu current = getById(entity.getId());
        if (current == null) {
            throw new BuzzException("菜单不存在");
        }
        String configKey = StrUtil.trim(entity.getConfigKey());
        if (StrUtil.isBlank(configKey)) {
            configKey = current.getConfigKey();
        } else if (!ObjectUtil.equal(configKey, current.getConfigKey())) {
            throw new BuzzException("菜单配置标识不可修改");
        }
        entity.setConfigKey(configKey);
        validateConfigKey(configKey, entity.getId());

        boolean requiredChanged = entity.getTenantRequired() != null
                && !ObjectUtil.equal(current.getTenantRequired(), entity.getTenantRequired());
        if (requiredChanged) {
            checkTenantRequiredPermission();
        }

        if (ObjectUtil.equal(entity.getParentId(), entity.getId())) {
            throw new BuzzException("父节点不能是自身");
        }

        // 不能选择本节点的子节点
        List<RbacMenu> subMenus = this.getAllChildrenFromNode(entity.getId());
        Optional<RbacMenu> optional = subMenus.stream().filter(i -> ObjectUtil.equal(i.getId(), entity.getParentId())).findFirst();
        if (optional.isPresent()) {
            throw new BuzzException("父节点不能选择本节点的子节点");
        }

//        long count = lambdaQuery().eq(RbacMenu::getLinkUrl, entity.getLinkUrl()).ne(RbacMenu::getId, entity.getId()).count();
//        if (count > 0) throw new BuzzException("链接已存在，不可重复录入");

        boolean updated = super.updateById(entity);
        if (updated && requiredChanged) {
            syncTenantRequiredPermission(
                    current,
                    Boolean.TRUE.equals(current.getTenantRequired()),
                    Boolean.TRUE.equals(entity.getTenantRequired())
            );
        }
        return updated;
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void moveUp(Serializable id) {
        super.moveUp(id);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void moveDown(Serializable id) {
        super.moveDown(id);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public void changePos(List<TreePosChangeVo> list) {
        super.changePos(list);
    }

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean removeById(Serializable id) {
        return super.removeById(id);
    }

    public List<FaOption<String>> getFlowMenuList() {
        return faFlowService.getFlowMenuList();
    }

    @Override
    protected void saveBefore(RbacMenu entity) {
        if (entity.getId() == null && StrUtil.isBlank(entity.getConfigKey())) {
            entity.setConfigKey(generateConfigKey());
        }
        if (entity.getId() == null && entity.getTenantRequired() == null) {
            entity.setTenantRequired(false);
        }
    }

    private String generateConfigKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateLegacyConfigKey(RbacMenu menu) {
        Integer scope = menu.getScope() == null ? RbacMenuScopeEnum.WEB.getValue() : menu.getScope().getValue();
        return "legacy:" + scope + ":" + menu.getId();
    }

    private void validateConfigKey(String configKey, Long excludedId) {
        if (StrUtil.isBlank(configKey)) {
            throw new BuzzException("菜单配置标识不能为空");
        }
        if (configKey.length() > CONFIG_KEY_MAX_LENGTH) {
            throw new BuzzException("菜单配置标识长度不能超过" + CONFIG_KEY_MAX_LENGTH + "个字符");
        }
        long count = excludedId == null
                ? lambdaQuery().eq(RbacMenu::getConfigKey, configKey).count()
                : lambdaQuery().eq(RbacMenu::getConfigKey, configKey).ne(RbacMenu::getId, excludedId).count();
        if (count > 0) {
            throw new BuzzException("菜单配置标识已存在: " + configKey);
        }
    }

    private void checkTenantRequiredPermission() {
        if (isTenantEnabled() && !isSuperAdminUser(getCurrentUserId())) {
            throw new BuzzException("仅平台管理员可维护租户必选权限");
        }
    }

    private void syncTenantRequiredPermission(RbacMenu menu, boolean oldRequired, boolean newRequired) {
        if (tenantPermissionBiz != null) {
            tenantPermissionBiz.syncRequiredPermissionChange(menu.getId(), oldRequired, newRequired);
        }
        if (entityLogBiz != null) {
            entityLogBiz.saveMsgLog(menu, "平台必选权限变更: " + oldRequired + " -> " + newRequired);
        }
    }
}
