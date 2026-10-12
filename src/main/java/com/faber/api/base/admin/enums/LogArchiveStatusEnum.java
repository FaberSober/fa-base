package com.faber.api.base.admin.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/** 日志归档执行状态。 */
@Getter
public enum LogArchiveStatusEnum implements IEnum<String> {
    PREPARING("PREPARING", "准备中"),
    ARCHIVING("ARCHIVING", "归档中"),
    SUCCESS("SUCCESS", "成功"),
    FAILED("FAILED", "失败"),
    CLEANED("CLEANED", "已清理");

    @JsonValue
    @EnumValue
    private final String value;
    private final String desc;

    LogArchiveStatusEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
