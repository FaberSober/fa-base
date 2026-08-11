package com.faber.api.base.admin.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 通用分类所属业务模块。
 */
@Getter
public enum CategoryModuleEnum implements IEnum<String> {
    AI_AGENT("AI_AGENT", "AI智能体"),
    AI_KB("AI_KB", "AI知识库"),
    AI_MCP("AI_MCP", "MCP服务"),
    AI_NL2SQL_DATASET("AI_NL2SQL_DATASET", "AI问数数据集");

    @JsonValue
    @EnumValue
    private final String value;
    private final String desc;

    CategoryModuleEnum(String value, String desc) {
        this.value = value;
        this.desc = desc;
    }
}
