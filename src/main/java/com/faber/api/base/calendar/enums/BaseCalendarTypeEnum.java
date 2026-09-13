package com.faber.api.base.calendar.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 统一日历的业务类型。 */
@Getter
public enum BaseCalendarTypeEnum implements IEnum<String> {

    OA("OA", "OA 工作日历"),
    TRADING("TRADING", "交易日历");

    @JsonValue
    @EnumValue
    private final String value;
    private final String desc;

    BaseCalendarTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
