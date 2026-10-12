package com.faber.api.base.calendar.vo;

import com.faber.api.base.calendar.entity.BaseCalendarDay;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 年度日历发布前的差异摘要。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDayImportPreviewVo {

    private String calendarCode;
    private int total;
    private int added;
    private int changed;
    private int unchanged;
    private List<BaseCalendarDay> addedDays;
    private List<BaseCalendarDay> changedDays;
}
