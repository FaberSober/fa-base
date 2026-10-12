package com.faber.api.base.admin.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * BASE-系统版本更新日志表
 * 
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-08-17 17:10:02
 */
@FaModalName(name = "BASE-系统版本更新日志表")
@TableName("base_system_update_log")
@Data
public class SystemUpdateLog implements Serializable {
	private static final long serialVersionUID = 1L;
	
    @ExcelProperty("ID")
    @TableId(type = IdType.AUTO)
    private Integer id;

    @ExcelProperty("模块编码")
    private String no;

    @ExcelProperty("模块名称")
    private String name;

    @ExcelProperty("版本号")
    @SqlEquals
    private Long ver;

    @ExcelProperty("版本编码")
    private String verNo;

    @ExcelProperty("备注信息")
    private String remark;

    @ExcelIgnore
    private String log;

    @ExcelProperty("升级日期")
    private Date crtTime;

    @ExcelProperty("执行状态")
    @SqlEquals
    @TableField(select = false)
    private Integer status;

    @ExcelProperty("SQL文件")
    @TableField(select = false)
    private String fileName;

    @ExcelProperty("内容校验和")
    @TableField(select = false)
    private String checksum;

    @ExcelProperty("执行耗时(ms)")
    @TableField(select = false)
    private Long durationMs;

    @ExcelIgnore
    @TableField(select = false, updateStrategy = FieldStrategy.ALWAYS)
    private String errorMsg;

}
