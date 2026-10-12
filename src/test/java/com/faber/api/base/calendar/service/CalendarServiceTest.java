package com.faber.api.base.calendar.service;

import com.faber.api.base.calendar.entity.BaseCalendar;
import com.faber.api.base.calendar.entity.BaseCalendarDay;
import com.faber.api.base.calendar.enums.BaseCalendarTypeEnum;
import com.faber.api.base.calendar.mapper.BaseCalendarDayMapper;
import com.faber.api.base.calendar.mapper.BaseCalendarMapper;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock
    private BaseCalendarMapper calendarMapper;

    @Mock
    private BaseCalendarDayMapper calendarDayMapper;

    @InjectMocks
    private CalendarServiceImpl calendarService;

    @Test
    void fallsBackToWeekdayWhenDateFactIsMissing() {
        givenEnabled(CalendarService.CN_A_SHARE);
        when(calendarDayMapper.selectActiveByCalendarCodeAndDate(
                eq(CalendarService.CN_A_SHARE), any(LocalDate.class), eq(false))).thenReturn(null);

        assertTrue(calendarService.isTradingDay("SH", LocalDate.of(2026, 9, 14)));
        assertFalse(calendarService.isTradingDay("SZ", LocalDate.of(2026, 9, 13)));
    }

    @Test
    void dateFactOverridesWeekdayFallback() {
        givenEnabled(CalendarService.CN_OA);
        BaseCalendarDay makeupDay = new BaseCalendarDay();
        makeupDay.setIsOpen(true);
        when(calendarDayMapper.selectActiveByCalendarCodeAndDate(
                CalendarService.CN_OA, LocalDate.of(2026, 9, 13), false)).thenReturn(makeupDay);

        assertTrue(calendarService.isWorkday(LocalDate.of(2026, 9, 13)));
    }

    @Test
    void previousOpenDateSkipsClosedDate() {
        givenEnabled(CalendarService.CN_A_SHARE);
        when(calendarDayMapper.selectActiveByCalendarCodeAndDate(
                eq(CalendarService.CN_A_SHARE), any(LocalDate.class), eq(false)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    if (date.equals(LocalDate.of(2026, 9, 14))) {
                        BaseCalendarDay closedDay = new BaseCalendarDay();
                        closedDay.setIsOpen(false);
                        return closedDay;
                    }
                    return null;
                });

        assertEquals(LocalDate.of(2026, 9, 11), calendarService.previousOrSameOpenDate(
                CalendarService.CN_A_SHARE, LocalDate.of(2026, 9, 14)));
    }

    @Test
    void mapsMarketsToIndependentCalendars() {
        assertEquals(CalendarService.CN_A_SHARE, calendarService.calendarCodeForMarket("sh"));
        assertEquals(CalendarService.CN_A_SHARE, calendarService.calendarCodeForMarket("SZ"));
        assertEquals(CalendarService.HK_STOCK, calendarService.calendarCodeForMarket("HK"));
        assertEquals(CalendarService.US_STOCK, calendarService.calendarCodeForMarket("US"));
    }

    @Test
    void rejectsUnknownMarketAndCalendar() {
        assertThrows(BuzzException.class,
                () -> calendarService.calendarCodeForMarket("JP"));
        when(calendarMapper.selectEnabledByCode("CN_OA", true, false)).thenReturn(null);
        assertThrows(BuzzException.class,
                () -> calendarService.isOpen("cn_oa", LocalDate.of(2026, 9, 14)));
    }

    private void givenEnabled(String code) {
        when(calendarMapper.selectEnabledByCode(code, true, false)).thenReturn(calendar(code));
    }

    private BaseCalendar calendar(String code) {
        BaseCalendar calendar = new BaseCalendar();
        calendar.setCalendarCode(code);
        calendar.setCalendarType(BaseCalendarTypeEnum.TRADING);
        calendar.setEnabled(true);
        return calendar;
    }
}
