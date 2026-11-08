package com.faber.api.base.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import com.alibaba.excel.annotation.ExcelProperty;
import java.util.Date;

/** 执行日志表，无逻辑删除与配置审计字段。 */
@Data
@TableName("base_job_flow_node_run")
public class JobFlowNodeRun {
    @ExcelProperty("节点运行ID")
    @TableId(type = IdType.AUTO)
    private Integer id;
    @ExcelProperty("所属流程执行记录ID")
    private Integer flowLogId;
    @ExcelProperty("流程内独立节点ID")
    private String nodeId;
    @ExcelProperty("执行类路径快照")
    private String clazzPath;
    @ExcelProperty("节点名称快照")
    private String nodeName;
    @ExcelProperty("尝试序号（首版固定1）")
    private Integer attempt;
    @ExcelProperty("业务执行日志ID（未启动为空）")
    private Integer jobLogId;
    @ExcelProperty("节点状态:0等待依赖/10待调度/20启动中/30执行中/40成功/50失败/60阻断/70停止/80未确认")
    private Integer status;
    @ExcelProperty("节点开始时间")
    private Date beginTime;
    @ExcelProperty("节点结束时间（未确认不填）")
    private Date endTime;
    @ExcelProperty("失败、阻断、停止或未确认原因")
    private String reason;
    @ExcelProperty("创建时间")
    private Date crtTime;
    @ExcelProperty("更新时间")
    private Date updTime;
}
