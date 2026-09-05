package com.faber.api.base.telemetry.listener;

import com.faber.api.base.telemetry.event.TelemetryStatReceivedEvent;
import com.faber.api.base.telemetry.service.TelemetryStatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/** 异步写入业务统计，不影响 Collector 响应。 */
@Slf4j
@Component
public class TelemetryStatListener {
    private final TelemetryStatService telemetryStatService;

    public TelemetryStatListener(TelemetryStatService telemetryStatService) {
        this.telemetryStatService = telemetryStatService;
    }

    @Async("executor")
    @EventListener
    public void onStatReceived(TelemetryStatReceivedEvent event) {
        try {
            telemetryStatService.persist(event.app(), event.request());
        } catch (Exception e) {
            log.warn("Telemetry 统计事件写入失败，appKey：{}", event.app().getAppKey(), e);
        }
    }
}
