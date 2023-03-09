package com.faber.api.base.generator.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 代码生成-类型
 */
@Getter
public enum GeneratorTypeEnum implements IEnum<String> {
    JAVA_ENTITY("java.entity", "entity.java.vm");

    @JsonValue
    @EnumValue
    private final String value;
    private final String desc;

    GeneratorTypeEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }

}
