package com.faber.api.base.rbac.biz;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.api.base.rbac.entity.RbacMenu;
//import com.alicp.jetcache.anno.Cached;
import com.faber.api.base.rbac.enums.RbacMenuScopeEnum;
import com.faber.api.base.rbac.mapper.RbacMenuMapper;
import com.faber.api.base.rbac.vo.query.RbacMenuExportReqVo;
import com.faber.api.base.rbac.vo.ret.RbacMenuExportVo;
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

//    @Cached(name="rbac:allMenuTree", key="new String('')", expire = 3600)
    @Override
    public List<TreeNode<RbacMenu>> allTree() {
        return super.allTree();
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

    @FaCacheClear(pre = "rbac:")
    @Override
    public boolean save(RbacMenu entity) {
        if (StrUtil.isBlank(entity.getConfigKey())) {
            entity.setConfigKey(generateConfigKey());
        } else {
            entity.setConfigKey(entity.getConfigKey().trim());
        }
        validateConfigKey(entity.getConfigKey(), entity.getId());
//        long count = lambdaQuery().eq(RbacMenu::getLinkUrl, entity.getLinkUrl()).count();
//        if (count > 0) throw new BuzzException("链接已存在，不可重复录入");

        return super.save(entity);
    }

    @FaCacheClear(pre = "rbac:")
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

        return super.updateById(entity);
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
    }

    private String generateConfigKey() {
        return UUID.randomUUID().toString().replace("-", "");
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
}
