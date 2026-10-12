package com.faber.api.base.admin.vo.query;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 部门专用导入请求。 */
@Data
public class DepartmentImportReqVo {

    @NotBlank
    private String fileId;
}
