package com.faber.api.base.calendar.importer;

import com.faber.api.base.calendar.biz.BaseCalendarDayBiz;
import com.faber.api.base.calendar.enums.BaseCalendarDayTypeEnum;
import com.faber.api.base.calendar.service.CalendarService;
import com.faber.api.base.calendar.vo.CalendarDayImportItem;
import com.faber.api.base.calendar.vo.CalendarDayImportPreviewVo;
import com.faber.api.base.calendar.vo.CalendarDayImportReq;
import com.faber.api.base.calendar.vo.CalendarExternalImportPreviewVo;
import com.faber.api.base.calendar.vo.CalendarExternalImportPublishReq;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarExternalImportServiceTest {

    @Mock
    private CalendarExternalSourceClient sourceClient;

    @Mock
    private BaseCalendarDayBiz calendarDayBiz;

    @Test
    void buildsIndependentOaAndAShareCalendars() {
        LocalDate newYear = LocalDate.of(2026, 1, 1);
        LocalDate makeupWorkday = LocalDate.of(2026, 1, 4);
        LocalDate specialTradingDay = LocalDate.of(2026, 1, 10);
        CalendarExternalSourceData sourceData = new CalendarExternalSourceData(
                2026,
                Map.of(
                        newYear, new CalendarExternalSourceData.HolidayDay(newYear, "元旦", true),
                        makeupWorkday, new CalendarExternalSourceData.HolidayDay(makeupWorkday, "元旦调休", false)),
                Set.of(
                        LocalDate.of(2026, 1, 2),
                        LocalDate.of(2026, 1, 5),
                        LocalDate.of(2026, 1, 6),
                        LocalDate.of(2026, 1, 7),
                        LocalDate.of(2026, 1, 8),
                        LocalDate.of(2026, 1, 9),
                        specialTradingDay),
                "HOLIDAY_CN",
                "2026",
                "AKTOOLS_AKSHARE",
                "2026");
        when(sourceClient.fetch(2026)).thenReturn(sourceData);
        when(calendarDayBiz.preview(any(CalendarDayImportReq.class))).thenAnswer(invocation -> {
            CalendarDayImportReq request = invocation.getArgument(0);
            int total = request.getDays().size();
            return new CalendarDayImportPreviewVo(request.getCalendarCode(), total, 0, 0, total, List.of(), List.of());
        });

        CalendarExternalImportPreviewVo result = new CalendarExternalImportService(sourceClient, calendarDayBiz).preview(2026);

        assertEquals(2, result.getCalendars().size());
        assertEquals(2, result.getImports().size());
        assertEquals(2, result.getImports().get(0).getDays().size());
        assertEquals(2, result.getImports().get(1).getDays().size());

        CalendarDayImportReq oa = result.getImports().stream()
                .filter(item -> CalendarService.CN_OA.equals(item.getCalendarCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(BaseCalendarDayTypeEnum.HOLIDAY, dayType(oa, newYear));
        assertFalse(isOpen(oa, newYear));
        assertEquals(BaseCalendarDayTypeEnum.MAKEUP_WORKDAY, dayType(oa, makeupWorkday));
        assertTrue(isOpen(oa, makeupWorkday));
        assertTrue(oa.getDays().stream().noneMatch(item ->
                item.getDayType() == BaseCalendarDayTypeEnum.WORKDAY
                        || item.getDayType() == BaseCalendarDayTypeEnum.WEEKEND));

        CalendarDayImportReq aShare = result.getImports().stream()
                .filter(item -> CalendarService.CN_A_SHARE.equals(item.getCalendarCode()))
                .findFirst()
                .orElseThrow();
        assertEquals(BaseCalendarDayTypeEnum.EXCHANGE_CLOSED, dayType(aShare, newYear));
        assertFalse(isOpen(aShare, newYear));
        assertTrue(aShare.getDays().stream().noneMatch(item -> makeupWorkday.equals(item.getCalendarDate())));
        assertEquals(BaseCalendarDayTypeEnum.SPECIAL_TRADING_DAY, dayType(aShare, specialTradingDay));
        assertTrue(isOpen(aShare, specialTradingDay));
        assertTrue(aShare.getDays().stream().noneMatch(item ->
                item.getDayType() == BaseCalendarDayTypeEnum.WORKDAY
                        || item.getDayType() == BaseCalendarDayTypeEnum.WEEKEND));
    }

    @Test
    void rejectsPartialExternalPublish() {
        CalendarExternalImportPublishReq request = new CalendarExternalImportPublishReq();
        request.setYear(2026);
        request.setImports(List.of(new CalendarDayImportReq()));

        CalendarExternalImportService service = new CalendarExternalImportService(sourceClient, calendarDayBiz);

        assertThrows(BuzzException.class, () -> service.publish(request));
    }

    @Test
    void rejectsDefaultDayTypesForExternalPublish() {
        CalendarDayImportItem normalWorkday = new CalendarDayImportItem();
        normalWorkday.setCalendarDate(LocalDate.of(2026, 1, 2));
        normalWorkday.setDayType(BaseCalendarDayTypeEnum.WORKDAY);
        normalWorkday.setIsOpen(true);

        CalendarDayImportReq oa = new CalendarDayImportReq();
        oa.setCalendarCode(CalendarService.CN_OA);
        oa.setDays(List.of(normalWorkday));

        CalendarDayImportReq aShare = new CalendarDayImportReq();
        aShare.setCalendarCode(CalendarService.CN_A_SHARE);
        aShare.setDays(List.of(normalWorkday));

        CalendarExternalImportPublishReq request = new CalendarExternalImportPublishReq();
        request.setYear(2026);
        request.setImports(List.of(oa, aShare));

        CalendarExternalImportService service = new CalendarExternalImportService(sourceClient, calendarDayBiz);

        assertThrows(BuzzException.class, () -> service.publish(request));
    }

    private BaseCalendarDayTypeEnum dayType(CalendarDayImportReq request, LocalDate date) {
        return request.getDays().stream()
                .filter(item -> date.equals(item.getCalendarDate()))
                .findFirst()
                .orElseThrow()
                .getDayType();
    }

    private boolean isOpen(CalendarDayImportReq request, LocalDate date) {
        return request.getDays().stream()
                .filter(item -> date.equals(item.getCalendarDate()))
                .findFirst()
                .orElseThrow()
                .getIsOpen();
    }
}
