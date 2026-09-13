package com.faber.api.base.calendar.importer;

/** 获取外部年度日历源数据。 */
public interface CalendarExternalSourceClient {

    CalendarExternalSourceData fetch(int year);
}
