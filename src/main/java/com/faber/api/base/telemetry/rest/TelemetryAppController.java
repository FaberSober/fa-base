package com.faber.api.base.telemetry.rest;

import com.faber.api.base.telemetry.biz.TelemetryAppBiz;
import com.faber.api.base.telemetry.entity.TelemetryApp;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.web.rest.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Telemetry 应用管理接口。 */
@FaLogBiz("Telemetry 应用")
@RestController
@RequestMapping("/api/base/telemetry/app")
public class TelemetryAppController extends BaseController<TelemetryAppBiz, TelemetryApp, Long> {
}
