package com.faber.api.base.telemetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.JsonNode;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.annotation.SqlSearch;
import com.faber.core.config.mybatis.handler.UniversalJsonTypeHandler;
import lombok.Data;

import java.util.Date;

/** 每次客户端异常的原始事件。 */
@Data
@TableName(value = "base_client_error_event", autoResultMap = true)
public class ClientErrorEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    @SqlEquals
    private Long appId;

    @SqlEquals
    private Long issueId;

    @SqlEquals
    private TelemetryClientTypeEnum clientType;

    @SqlEquals
    private String environment;

    @SqlEquals
    private String release;

    @SqlEquals
    private String sessionId;

    @SqlEquals
    private String userId;

    @SqlEquals
    private String tenantId;

    @SqlEquals
    private String errorType;

    @SqlSearch
    private String message;

    private String stack;

    @TableField(typeHandler = UniversalJsonTypeHandler.class)
    private JsonNode breadcrumbs;

    @TableField(typeHandler = UniversalJsonTypeHandler.class)
    private JsonNode context;

    private Date occurTime;
    private Date createTime;
}
