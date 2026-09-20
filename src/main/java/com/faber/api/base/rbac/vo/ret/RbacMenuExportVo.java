package com.faber.api.base.rbac.vo.ret;

import com.faber.api.base.rbac.entity.RbacMenu;
import com.faber.api.base.rbac.enums.RbacLinkTypeEnum;
import com.faber.api.base.rbac.enums.RbacMenuLevelEnum;
import com.faber.api.base.rbac.enums.RbacMenuScopeEnum;
import lombok.Data;

import java.util.List;

/** 菜单 JSON 导出数据。 */
@Data
public class RbacMenuExportVo {

    private String schema;
    private Integer version;
    private RbacMenuScopeEnum scope;
    private String exportedAt;
    private List<Node> nodes;

    @Data
    public static class Node {
        private String configKey;
        private Long id;
        private String parentConfigKey;
        private String name;
        private Integer sort;
        private RbacMenuLevelEnum level;
        private String icon;
        private Boolean status;
        private RbacLinkTypeEnum linkType;
        private String linkUrl;

        public static Node from(RbacMenu menu, String parentConfigKey) {
            Node node = new Node();
            node.configKey = menu.getConfigKey();
            node.id = menu.getId();
            node.parentConfigKey = parentConfigKey;
            node.name = menu.getName();
            node.sort = menu.getSort();
            node.level = menu.getLevel();
            node.icon = menu.getIcon();
            node.status = menu.getStatus();
            node.linkType = menu.getLinkType();
            node.linkUrl = menu.getLinkUrl();
            return node;
        }
    }
}
