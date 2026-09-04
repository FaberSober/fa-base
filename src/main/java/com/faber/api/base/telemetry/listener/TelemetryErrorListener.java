package com.faber.api.base.telemetry.listener;

import com.faber.api.base.telemetry.event.TelemetryErrorReceivedEvent;
import com.faber.api.base.telemetry.service.TelemetryErrorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** 异步写入异常数据，写入失败不会影响 Collector 请求。 */
@Slf4j
@Component
public class TelemetryErrorListener {

    private final TelemetryErrorService telemetryErrorService;

    public TelemetryErrorListener(TelemetryErrorService telemetryErrorService) {
        this.telemetryErrorService = telemetryErrorService;
    }

    @Async("executor")
    @EventListener
    public void onErrorReceived(TelemetryErrorReceivedEvent event) {
        try {
            telemetryErrorService.persist(event.app(), event.request());
        } catch (Exception e) {
            log.warn("Telemetry 异常事件写入失败，appKey：{}", event.app().getAppKey(), e);
        }
    }
}
