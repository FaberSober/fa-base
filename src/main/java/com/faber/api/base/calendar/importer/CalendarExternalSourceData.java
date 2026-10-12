package com.faber.api.base.calendar.importer;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 外部日历源数据的规范化结果。 */
public record CalendarExternalSourceData(
        int year,
        Map<LocalDate, HolidayDay> holidayDays,
        Set<LocalDate> aShareTradingDates,
        String holidaySource,
        String holidaySourceVersion,
        String aShareSource,
        String aShareSourceVersion) {

    public CalendarExternalSourceData {
        holidayDays = Map.copyOf(Objects.requireNonNull(holidayDays, "holidayDays"));
        aShareTradingDates = Set.copyOf(Objects.requireNonNull(aShareTradingDates, "aShareTradingDates"));
    }

    /** holiday-cn 中的一条节假日/调休事实。 */
    public record HolidayDay(LocalDate date, String name, boolean offDay) {
    }
}
