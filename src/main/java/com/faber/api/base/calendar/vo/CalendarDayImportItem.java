package com.faber.api.base.calendar.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faber.api.base.calendar.enums.BaseCalendarDayTypeEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 年度日历导入中的单日记录。 */
@Data
public class CalendarDayImportItem {

    @NotNull
    private LocalDate calendarDate;

    @NotNull
    private BaseCalendarDayTypeEnum dayType;

    @NotNull
    @JsonProperty("isOpen")
    private Boolean isOpen;

    @Size(max = 128)
    private String holidayName;

    @Size(max = 512)
    private String remark;
}
