package com.faber.api.base.rbac.vo.ret;

import lombok.Data;

/** 菜单 JSON 导入结果。 */
@Data
public class RbacMenuImportResultVo {

    private int createdCount;
    private int updatedCount;
    private int unchangedCount;
}
