package com.faber.api.base.admin.vo.ret;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 部门导入预览摘要。 */
@Data
public class DepartmentImportPreviewVo {

    private int totalCount;
    private int validCount;
    private int errorCount;
    private int createCount;
    private int updateCount;
    private List<DepartmentImportErrorVo> errors = new ArrayList<>();
}
