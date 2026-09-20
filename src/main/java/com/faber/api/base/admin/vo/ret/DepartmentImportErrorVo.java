package com.faber.api.base.admin.vo.ret;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 部门导入单行校验错误。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentImportErrorVo {

    private int rowNumber;
    private String departmentName;
    private String message;
}
