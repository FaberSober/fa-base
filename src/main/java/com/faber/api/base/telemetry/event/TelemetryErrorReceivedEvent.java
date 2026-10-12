package com.faber.api.base.telemetry.event;

import com.faber.api.base.telemetry.entity.TelemetryApp;
import com.faber.api.base.telemetry.vo.TelemetryErrorReq;

/** Collector 校验通过后的异常事件，供异步持久化使用。 */
public record TelemetryErrorReceivedEvent(TelemetryApp app, TelemetryErrorReq request) {
}
