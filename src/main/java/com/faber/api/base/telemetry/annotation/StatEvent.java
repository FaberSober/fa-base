package com.faber.api.base.telemetry.annotation;

import com.faber.api.base.telemetry.enums.TelemetryStatEventTypeEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 在业务方法成功返回后记录 Telemetry 统计事件。 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StatEvent {

    String value();

    TelemetryStatEventTypeEnum eventType() default TelemetryStatEventTypeEnum.BUSINESS;

    String module() default "";

    String bizType() default "";
}
