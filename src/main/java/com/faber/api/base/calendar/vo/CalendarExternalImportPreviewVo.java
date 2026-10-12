package com.faber.api.base.calendar.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 外部年度日历预览及待发布数据。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarExternalImportPreviewVo {

    private int year;
    private List<CalendarExternalImportCalendarPreviewVo> calendars;
    private List<CalendarDayImportReq> imports;
}
