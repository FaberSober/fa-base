package com.faber.api.base.admin.vo.query;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 用户专用导入请求。 */
@Data
public class UserImportReqVo {

    @NotBlank
    private String fileId;

    /** 普通用户管理页面当前选中的部门范围。 */
    @Size(max = 64)
    private String departmentIdSuper;

    /** 是否使用平台超级用户的跨租户导入范围。 */
    private Boolean superMode;
}
