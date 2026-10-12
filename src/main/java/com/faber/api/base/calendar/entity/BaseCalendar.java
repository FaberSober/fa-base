package com.faber.api.base.calendar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.api.base.calendar.enums.BaseCalendarTypeEnum;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.annotation.SqlSearch;
import com.faber.core.bean.BaseDelEntity;
import com.faber.core.config.validator.validator.Vg;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 统一工作日/交易日日历定义。 */
@Data
@EqualsAndHashCode(callSuper = true)
@FaModalName(name = "统一日历定义")
@TableName("base_calendar")
public class BaseCalendar extends BaseDelEntity {

    @Null(groups = Vg.Crud.C.class)
    @NotNull(groups = Vg.Crud.U.class)
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank
    @Size(max = 32)
    @SqlEquals
    private String calendarCode;

    @NotBlank
    @Size(max = 64)
    @SqlSearch
    private String name;

    @NotNull
    @SqlEquals
    private BaseCalendarTypeEnum calendarType;

    @Size(max = 16)
    @SqlEquals
    private String market;

    @NotBlank
    @Size(max = 64)
    private String timezone;

    @NotNull
    @SqlEquals
    private Boolean enabled;
}
