package com.faber.api.base.telemetry.vo;

import com.fasterxml.jackson.databind.JsonNode;
import com.faber.api.base.telemetry.enums.TelemetryStatEventTypeEnum;
import lombok.Data;

/** 服务端业务代码调用 TelemetryService 时提供的事件信息。 */
@Data
public class TelemetryTrackCommand {

    private TelemetryStatEventTypeEnum eventType = TelemetryStatEventTypeEnum.BUSINESS;
    private String eventCode;
    private String module;
    private String bizType;
    private String bizId;
    private String result = "SUCCESS";
    private Long duration;
    private JsonNode properties;
}
