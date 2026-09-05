package com.faber.api.base.telemetry.event;

import com.faber.api.base.telemetry.entity.TelemetryApp;
import com.faber.api.base.telemetry.vo.TelemetryEventReq;

/** Collector 校验通过后的统计事件。 */
public record TelemetryStatReceivedEvent(TelemetryApp app, TelemetryEventReq request) {
}
