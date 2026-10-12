package com.faber.api.base.rbac.vo.query;

import com.faber.api.base.rbac.enums.RbacMenuScopeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 菜单 JSON 导出请求。 */
@Data
public class RbacMenuExportReqVo {

    @NotNull
    private RbacMenuScopeEnum scope;
}
