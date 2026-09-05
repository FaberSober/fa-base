package com.faber.api.base.telemetry.aspect;

import com.faber.api.base.telemetry.annotation.StatEvent;
import com.faber.api.base.telemetry.service.TelemetryService;
import com.faber.api.base.telemetry.vo.TelemetryTrackCommand;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/** 仅在标注方法正常结束后发布统计事件。 */
@Aspect
@Component
public class StatEventAspect {

    private final TelemetryService telemetryService;

    public StatEventAspect(TelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    @Around("@annotation(statEvent)")
    public Object track(ProceedingJoinPoint joinPoint, StatEvent statEvent) throws Throwable {
        long start = System.nanoTime();
        Object result = joinPoint.proceed();
        TelemetryTrackCommand command = new TelemetryTrackCommand();
        command.setEventType(statEvent.eventType());
        command.setEventCode(statEvent.value());
        command.setModule(emptyToNull(statEvent.module()));
        command.setBizType(emptyToNull(statEvent.bizType()));
        command.setDuration(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
        telemetryService.track(command);
        return result;
    }

    private String emptyToNull(String value) {
        return value.isBlank() ? null : value;
    }
}
