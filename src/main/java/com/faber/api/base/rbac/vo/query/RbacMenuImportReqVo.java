package com.faber.api.base.rbac.vo.query;

import com.faber.api.base.rbac.enums.RbacMenuScopeEnum;
import com.faber.api.base.rbac.vo.ret.RbacMenuExportVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 菜单 JSON 导入请求。 */
@Data
public class RbacMenuImportReqVo {

    /** 当前目标环境的菜单范围，避免误把 web 菜单导入 APP。 */
    @NotNull
    private RbacMenuScopeEnum scope;

    @NotNull
    @Valid
    private RbacMenuExportVo config;
}
