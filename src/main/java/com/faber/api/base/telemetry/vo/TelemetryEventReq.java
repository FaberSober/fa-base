package com.faber.api.base.telemetry.vo;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 业务统计事件上报协议；数据落库在 Phase 3 实现。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TelemetryEventReq extends TelemetryBaseReq {

    @NotBlank
    @Size(max = 32)
    private String eventType;

    @NotBlank
    @Size(max = 128)
    private String eventCode;

    @Size(max = 128)
    private String module;

    @Size(max = 128)
    private String bizType;

    @Size(max = 128)
    private String bizId;

    @Size(max = 32)
    private String result;

    private Long duration;

    private JsonNode properties;
}
