package com.faber.api.base.telemetry.vo;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 客户端异常上报协议；数据落库与 Issue 聚合在 Phase 2 实现。 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TelemetryErrorReq extends TelemetryBaseReq {

    @NotBlank
    @Size(max = 128)
    private String errorType;

    @NotBlank
    @Size(max = 2_000)
    private String message;

    @Size(max = 32_768)
    private String stack;

    private JsonNode breadcrumbs;
}
