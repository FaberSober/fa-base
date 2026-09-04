package com.faber.api.base.telemetry.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

/** Error 与业务事件共用的客户端上报上下文。 */
@Data
public class TelemetryBaseReq {

    @NotBlank
    @Size(max = 64)
    private String appKey;

    @NotNull
    private TelemetryClientTypeEnum clientType;

    @NotBlank
    @Pattern(regexp = "development|test|staging|production")
    private String environment;

    @NotBlank
    @Size(max = 128)
    private String release;

    @NotBlank
    @Size(max = 64)
    private String sessionId;

    @Size(max = 64)
    private String userId;

    @Size(max = 64)
    private String tenantId;

    private Date occurTime;

    private JsonNode context;

    /** 由服务端补充，不接受客户端传入。 */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Date receiveTime;
}
