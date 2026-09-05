package com.faber.api.base.telemetry.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 业务统计事件类型。 */
@Getter
public enum TelemetryStatEventTypeEnum implements IEnum<String> {
    LOGIN("LOGIN", "登录"),
    PAGE_VIEW("PAGE_VIEW", "页面访问"),
    ACTION("ACTION", "功能操作"),
    BUSINESS("BUSINESS", "业务事件");

    @JsonValue
    @EnumValue
    private final String value;
    private final String desc;

    TelemetryStatEventTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
