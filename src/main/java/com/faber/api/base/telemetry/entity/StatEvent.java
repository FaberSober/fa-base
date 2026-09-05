package com.faber.api.base.telemetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.api.base.telemetry.enums.TelemetryStatEventTypeEnum;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.annotation.SqlSearch;
import com.faber.core.config.mybatis.handler.UniversalJsonTypeHandler;
import lombok.Data;

import java.util.Date;

/** 用户行为与业务结果的原始统计事件。 */
@Data
@TableName(value = "base_stat_event", autoResultMap = true)
public class StatEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    @SqlEquals private Long appId;
    @SqlEquals private TelemetryClientTypeEnum clientType;
    @SqlEquals private String environment;
    @SqlEquals private String release;
    @SqlEquals private String sessionId;
    @SqlEquals private String userId;
    @SqlEquals private String tenantId;
    @SqlEquals private TelemetryStatEventTypeEnum eventType;
    @SqlSearch private String eventCode;
    @SqlEquals private String module;
    @SqlEquals private String bizType;
    @SqlEquals private String bizId;
    @SqlEquals private String result;
    private Long duration;
    @TableField(typeHandler = UniversalJsonTypeHandler.class) private JsonNode properties;
    @TableField(typeHandler = UniversalJsonTypeHandler.class) private JsonNode context;
    private Date occurTime;
    private Date createTime;
}
