package com.faber.api.base.telemetry.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 客户端异常 Issue 状态。 */
@Getter
public enum TelemetryIssueStatusEnum implements IEnum<String> {
    OPEN("OPEN", "待处理"),
    RESOLVED("RESOLVED", "已解决"),
    IGNORED("IGNORED", "已忽略");

    @JsonValue
    @EnumValue
    private final String value;
    private final String desc;

    TelemetryIssueStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
