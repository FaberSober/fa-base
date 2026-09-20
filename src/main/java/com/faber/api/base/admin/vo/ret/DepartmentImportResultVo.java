package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/** 部门导入提交结果。 */
@Data
public class DepartmentImportResultVo {

    private int totalCount;
    private int createCount;
    private int updateCount;
}
