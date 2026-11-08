package com.faber.api.base.admin.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.annotation.FaModalName;
import com.faber.core.bean.BaseCrtEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 不可变历史版本，只由流程保存事务创建，无通用编辑、删除入口。 */
@Data
@EqualsAndHashCode(callSuper = true)
@FaModalName(name = "流程定义历史版本")
@TableName("base_job_definition_version")
public class JobDefinitionVersion extends BaseCrtEntity {
    @TableId(type = IdType.AUTO)
    @ExcelProperty("版本ID")
    private Integer id;
    @ExcelProperty("所属流程任务ID")
    private Integer jobId;
    @ExcelProperty("任务内递增版本号")
    private Integer versionNo;
    @ExcelProperty("任务名称快照")
    private String jobName;
    @ExcelProperty("详细日志开关快照:false关闭/true开启")
    private Boolean detailLogEnabled;
    @ExcelProperty("不可变流程定义JSON（含节点位置）")
    private String flowDefinition;
    @ExcelProperty("规范化定义SHA-256（含名称与日志开关）")
    private String contentHash;
}
