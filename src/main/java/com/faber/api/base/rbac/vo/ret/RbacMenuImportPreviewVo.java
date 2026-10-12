package com.faber.api.base.rbac.vo.ret;

import com.faber.api.base.rbac.enums.RbacMenuScopeEnum;
import lombok.Data;

import java.util.List;

/** 菜单 JSON 导入差异预览。 */
@Data
public class RbacMenuImportPreviewVo {

    private RbacMenuScopeEnum scope;
    private int total;
    private int createCount;
    private int updateCount;
    private int moveCount;
    private int sortCount;
    private int unchangedCount;
    private int conflictCount;
    private int extraCount;
    private List<Change> changes;

    @Data
    public static class Change {
        private String type;
        private String configKey;
        private String name;
        private String detail;

        public Change() {
        }

        public Change(String type, String configKey, String name, String detail) {
            this.type = type;
            this.configKey = configKey;
            this.name = name;
            this.detail = detail;
        }
    }
}
