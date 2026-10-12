package com.faber.api.base.telemetry.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** Telemetry 客户端类型。 */
@Getter
public enum TelemetryClientTypeEnum implements IEnum<String> {
    WEB("WEB", "Web"),
    DESKTOP("DESKTOP", "桌面端"),
    MOBILE("MOBILE", "移动端"),
    OTHER("OTHER", "其他");

    @JsonValue
    @EnumValue
    private final String value;
    private final String desc;

    TelemetryClientTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
