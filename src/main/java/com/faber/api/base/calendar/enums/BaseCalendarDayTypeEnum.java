package com.faber.api.base.calendar.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 日历日期的最终业务分类。 */
@Getter
public enum BaseCalendarDayTypeEnum implements IEnum<String> {

    WORKDAY("WORKDAY", "工作日"),
    WEEKEND("WEEKEND", "周末"),
    HOLIDAY("HOLIDAY", "法定节假日"),
    MAKEUP_WORKDAY("MAKEUP_WORKDAY", "调休工作日"),
    EXCHANGE_CLOSED("EXCHANGE_CLOSED", "交易所休市日"),
    SPECIAL_TRADING_DAY("SPECIAL_TRADING_DAY", "特殊交易日");

    @JsonValue
    @EnumValue
    private final String value;
    private final String desc;

    BaseCalendarDayTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
