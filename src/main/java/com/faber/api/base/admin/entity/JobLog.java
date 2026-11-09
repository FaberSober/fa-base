package com.faber.api.base.admin.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;


/**
 * BASE-系统定时任务-执行日志
 * 
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-07 16:03:51
 */
@FaModalName(name = "BASE-系统定时任务-执行日志")
@TableName("base_job_log")
@Data
public class JobLog implements Serializable {
	private static final long serialVersionUID = 1L;
	
    @ExcelProperty("ID")
    @TableId(type = IdType.AUTO)
    private Integer id;

    @SqlEquals
    @ExcelProperty("任务配置ID（无独立配置的节点为空）")
    private Integer jobId;

    @ExcelProperty("本轮任务类型:0普通/1流程")
    private Integer jobType;
    @ExcelProperty("触发来源:0历史或未知/1手动/2Cron/3流程节点")
    private Integer triggerSource;
    @ExcelProperty("本轮流程定义版本ID（仅流程整体日志）")
    private Integer definitionVersionId;
    @ExcelProperty("执行类路径快照")
    private String clazzPath;
    @ExcelProperty("任务名称快照")
    private String jobName;

    @ExcelProperty("创建时间")
    private Date beginTime;

    @ExcelProperty("结束时间")
    private Date endTime;

    @ExcelProperty("执行结果:1执行中/2成功/3停止中/7已停止/8未确认/9失败")
    private JobLogStatusEnum status;

    @ExcelProperty("执行耗时(秒)")
    private Long duration;

    @ExcelProperty("错误日志")
    private String errMsg;

    @ExcelProperty("日志相对路径")
    private String logPath;

    @ExcelProperty("主流程返回时间")
    private Date dispatchEndTime;

    @ExcelProperty("触发耗时(ms)")
    private Long dispatchDurationMs;

    @ExcelProperty("总耗时(ms)")
    private Long durationMs;

    @ExcelProperty("统计口径:0旧/1完整/2未确认")
    private Integer trackingVersion;

}
