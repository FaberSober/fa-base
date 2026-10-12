package com.faber.api.base.telemetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.api.base.telemetry.enums.TelemetryStatEventTypeEnum;
import lombok.Data;

import java.util.Date;

/** Telemetry 按日聚合统计，原始明细保留在 StatEvent。 */
@Data
@TableName("base_stat_daily")
public class StatDaily {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Date statDate;
    private Long appId;
    private TelemetryClientTypeEnum clientType;
    private String environment;
    private TelemetryStatEventTypeEnum eventType;
    private String eventCode;
    private String module;
    private Long pv;
    private Long uv;
    private Long successCount;
    private Long failCount;
    private Double avgDuration;
    private Date createTime;
    private Date updateTime;
}
