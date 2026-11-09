package com.faber.api.base.admin.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.annotation.SqlSearch;
import com.faber.core.bean.BaseDelEntity;
import com.faber.config.quartz.vo.JobInfo;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;


/**
 * 系统定时任务
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2019-08-21 14:48:06
 */
@EqualsAndHashCode(callSuper = true)
@FaModalName(name = "定时任务")
@TableName("base_job")
@Data
public class Job extends BaseDelEntity implements JobInfo {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ExcelProperty("ID")
    private Integer id;

    @SqlSearch
    @ExcelProperty("任务名称")
    private String jobName;

    /** 0普通任务，1流程任务；历史数据为0。 */
    @SqlEquals
    @ExcelProperty("任务类型:0普通/1流程")
    private Integer jobType;

    @ExcelProperty("当前流程定义JSON（仅流程任务）")
    private String flowDefinition;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("当前流程定义版本ID（仅流程任务）")
    private Integer currentDefinitionVersionId;

    @SqlSearch
    @ExcelProperty("cron表达式")
    private String cron;

    @SqlEquals
    @ExcelProperty("是否启动")
    private Boolean status;

    @SqlSearch
    @ExcelProperty("任务执行方法")
    private String clazzPath;

    @ExcelProperty("任务描述")
    private String jobDesc;

    @ExcelProperty("记录详细日志")
    private Boolean detailLogEnabled = false;

    // 执行统计仅由受控 Mapper 更新，编辑、启停及 Excel 导入不能覆盖。
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("最近执行记录ID")
    private Integer lastJobLogId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("最近开始时间")
    private Date lastExecuteTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("最近结束时间")
    private Date lastEndTime;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("最近执行结果")
    private JobLogStatusEnum lastStatus;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("最近总耗时(ms)")
    private Long lastDurationMs;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("最近触发耗时(ms)")
    private Long lastDispatchDurationMs;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("最近统计口径:0旧/1完整/2未确认")
    private Integer lastTrackingVersion;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("累计执行次数")
    private Long executeCount = 0L;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("累计成功次数(含已知旧记录)")
    private Long successCount = 0L;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("累计失败次数(含已知旧记录)")
    private Long failCount = 0L;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("累计总耗时(ms)")
    private Long totalDurationMs = 0L;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("参与平均的执行次数")
    private Long durationSampleCount = 0L;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    @ExcelProperty("耗时统计起始时间")
    private Date statsSince;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @TableField(exist = false)
    @ExcelProperty("平均总耗时(ms)")
    private Long avgDurationMs;

    public Long getAvgDurationMs() {
        if (durationSampleCount == null || durationSampleCount <= 0 || totalDurationMs == null) return null;
        return BigDecimal.valueOf(totalDurationMs)
                .divide(BigDecimal.valueOf(durationSampleCount), 0, RoundingMode.HALF_UP).longValueExact();
    }

}
