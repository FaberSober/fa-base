package com.faber.api.base.calendar.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.api.base.calendar.enums.BaseCalendarDayTypeEnum;
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

import java.time.LocalDate;

/** 统一日历的单日最终事实。 */
@Data
@EqualsAndHashCode(callSuper = true)
@FaModalName(name = "统一日历日期")
@TableName("base_calendar_day")
public class BaseCalendarDay extends BaseDelEntity {

    @Null(groups = Vg.Crud.C.class)
    @NotNull(groups = Vg.Crud.U.class)
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank
    @Size(max = 32)
    @SqlEquals
    private String calendarCode;

    @NotNull
    @SqlEquals
    private LocalDate calendarDate;

    @NotNull
    @SqlEquals
    private BaseCalendarDayTypeEnum dayType;

    @NotNull
    @SqlEquals
    @TableField("is_open")
    private Boolean isOpen;

    @Size(max = 128)
    @SqlSearch
    private String holidayName;

    @Size(max = 64)
    @SqlEquals
    private String source;

    @Size(max = 64)
    @SqlEquals
    private String sourceVersion;

    @Size(max = 512)
    private String remark;
}
