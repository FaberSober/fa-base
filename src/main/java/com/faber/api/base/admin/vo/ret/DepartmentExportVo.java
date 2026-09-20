package com.faber.api.base.admin.vo.ret;

import com.alibaba.excel.annotation.ExcelProperty;
import com.faber.api.base.admin.entity.Department;
import com.faber.api.base.admin.entity.User;
import com.faber.core.annotation.FaModalName;
import lombok.Data;

import java.util.Date;

/**
 * 部门导出数据模型。
 */
@Data
@FaModalName(name = "部门")
public class DepartmentExportVo {

    @ExcelProperty("部门ID")
    private String id;

    @ExcelProperty("上级部门ID")
    private String parentId;

    @ExcelProperty("层级")
    private Integer level;

    @ExcelProperty("部门名称")
    private String name;

    @ExcelProperty("类型")
    private String type;

    @ExcelProperty("负责人ID")
    private String managerId;

    @ExcelProperty("负责人")
    private String managerName;

    @ExcelProperty("排序")
    private Integer sort;

    @ExcelProperty("描述")
    private String description;

    @ExcelProperty("创建时间")
    private Date crtTime;

    @ExcelProperty("更新时间")
    private Date updTime;

    public static DepartmentExportVo from(Department department, Integer level, User manager) {
        DepartmentExportVo vo = new DepartmentExportVo();
        vo.id = department.getId();
        vo.parentId = department.getParentId();
        vo.level = level;
        vo.name = department.getName();
        vo.type = department.getType();
        vo.managerId = department.getManagerId();
        vo.managerName = manager == null ? null : manager.getName();
        vo.sort = department.getSort();
        vo.description = department.getDescription();
        vo.crtTime = department.getCrtTime();
        vo.updTime = department.getUpdTime();
        return vo;
    }
}
