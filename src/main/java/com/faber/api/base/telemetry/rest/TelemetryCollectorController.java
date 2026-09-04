package com.faber.api.base.telemetry.rest;

import com.faber.api.base.telemetry.service.TelemetryCollectorService;
import com.faber.api.base.telemetry.vo.TelemetryErrorReq;
import com.faber.api.base.telemetry.vo.TelemetryEventReq;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 不依赖登录态的 Telemetry Collector 入口。 */
@RestController
@RequestMapping("/open/telemetry")
@IgnoreUserToken
public class TelemetryCollectorController extends BaseResHandler {

    private final TelemetryCollectorService collectorService;

    public TelemetryCollectorController(TelemetryCollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @PostMapping("/error")
    public Ret<Void> collectError(@Valid @RequestBody TelemetryErrorReq request) {
        collectorService.acceptError(request);
        return ok();
    }

    @PostMapping("/event")
    public Ret<Void> collectEvent(@Valid @RequestBody TelemetryEventReq request) {
        collectorService.acceptEvent(request);
        return ok();
    }
}
