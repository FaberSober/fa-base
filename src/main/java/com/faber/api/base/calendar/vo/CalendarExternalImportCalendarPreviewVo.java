package com.faber.api.base.calendar.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 外部导入的一套日历差异。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CalendarExternalImportCalendarPreviewVo {

    private String calendarCode;
    private String source;
    private String sourceVersion;
    private CalendarDayImportPreviewVo preview;
}
