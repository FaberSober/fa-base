package com.faber.api.base.telemetry.rest;

import com.faber.api.base.telemetry.service.TelemetryCollectorService;
import com.faber.api.base.telemetry.vo.TelemetryErrorReq;
import com.faber.api.base.telemetry.vo.TelemetryEventReq;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 不依赖登录态的 Telemetry Collector 入口。 */
@FaLogBiz("Telemetry 收集")
@RestController
@RequestMapping("/api/base/telemetry/open")
public class TelemetryCollectorController extends BaseResHandler {

    private final TelemetryCollectorService collectorService;

    public TelemetryCollectorController(TelemetryCollectorService collectorService) {
        this.collectorService = collectorService;
    }

    @IgnoreUserToken
    @FaLogOpr(value = "接收客户端异常", crud = LogCrudEnum.C)
    @PostMapping("/error")
    public Ret<Void> collectError(@Valid @RequestBody TelemetryErrorReq request) {
        collectorService.acceptError(request);
        return ok();
    }

    @IgnoreUserToken
    @FaLogOpr(value = "接收客户端事件", crud = LogCrudEnum.C)
    @PostMapping("/event")
    public Ret<Void> collectEvent(@Valid @RequestBody TelemetryEventReq request) {
        collectorService.acceptEvent(request);
        return ok();
    }
}
