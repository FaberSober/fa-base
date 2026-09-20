package com.faber.api.base.admin.vo.ret;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.faber.core.annotation.FaModalName;
import lombok.Data;

/** 部门导入模板与读取模型。 */
@Data
@FaModalName(name = "部门导入模板")
public class DepartmentImportRowVo {

    @ExcelProperty("部门ID")
    private String id;

    @ExcelProperty("上级部门ID")
    private String parentId;

    @ExcelProperty("部门名称")
    private String name;

    @ExcelProperty("类型")
    private String type;

    @ExcelProperty("负责人ID")
    private String managerId;

    @ExcelProperty("排序")
    private String sort;

    @ExcelProperty("描述")
    private String description;

    @ExcelIgnore
    private int rowNumber;
}
