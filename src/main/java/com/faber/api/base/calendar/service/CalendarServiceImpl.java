package com.faber.api.base.calendar.service;

import com.faber.api.base.calendar.entity.BaseCalendar;
import com.faber.api.base.calendar.entity.BaseCalendarDay;
import com.faber.api.base.calendar.mapper.BaseCalendarDayMapper;
import com.faber.api.base.calendar.mapper.BaseCalendarMapper;
import com.faber.core.exception.BuzzException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;

/** 统一日历查询服务实现。 */
@Service
public class CalendarServiceImpl implements CalendarService {

    private static final Boolean ENABLED = Boolean.TRUE;
    private static final Boolean NOT_DELETED = Boolean.FALSE;
    private static final int MAX_LOOKBACK_DAYS = 3660;

    private final BaseCalendarMapper calendarMapper;
    private final BaseCalendarDayMapper calendarDayMapper;

    public CalendarServiceImpl(BaseCalendarMapper calendarMapper,
                               BaseCalendarDayMapper calendarDayMapper) {
        this.calendarMapper = calendarMapper;
        this.calendarDayMapper = calendarDayMapper;
    }

    @Override
    public boolean isOpen(String calendarCode, LocalDate date) {
        LocalDate safeDate = requireDate(date);
        String safeCode = normalizeCalendarCode(calendarCode);
        requireEnabledCalendar(safeCode);

        BaseCalendarDay day = calendarDayMapper.selectActiveByCalendarCodeAndDate(
                safeCode, safeDate, NOT_DELETED);
        if (day != null && day.getIsOpen() != null) {
            return day.getIsOpen();
        }
        return isWeekday(safeDate);
    }

    @Override
    public boolean isTradingDay(String market, LocalDate date) {
        return isOpen(calendarCodeForMarket(market), date);
    }

    @Override
    public LocalDate previousOrSameOpenDate(String calendarCode, LocalDate date) {
        LocalDate result = requireDate(date);
        String safeCode = normalizeCalendarCode(calendarCode);
        requireEnabledCalendar(safeCode);
        for (int i = 0; i <= MAX_LOOKBACK_DAYS; i++) {
            if (isOpen(safeCode, result)) {
                return result;
            }
            result = result.minusDays(1);
        }
        throw new BuzzException("日历在指定日期之前没有可用开放日: " + safeCode);
    }

    @Override
    public String calendarCodeForMarket(String market) {
        String normalizedMarket = market == null ? "" : market.trim().toUpperCase(Locale.ROOT);
        return switch (normalizedMarket) {
            case "SH", "SZ" -> CN_A_SHARE;
            case "HK" -> HK_STOCK;
            case "US" -> US_STOCK;
            default -> throw new BuzzException("不支持的市场: " + market);
        };
    }

    private BaseCalendar requireEnabledCalendar(String calendarCode) {
        BaseCalendar calendar = calendarMapper.selectEnabledByCode(calendarCode, ENABLED, NOT_DELETED);
        if (calendar == null) {
            throw new BuzzException("日历不存在或未启用: " + calendarCode);
        }
        return calendar;
    }

    private String normalizeCalendarCode(String calendarCode) {
        if (calendarCode == null || calendarCode.isBlank()) {
            throw new BuzzException("日历编码不能为空");
        }
        return calendarCode.trim().toUpperCase(Locale.ROOT);
    }

    private LocalDate requireDate(LocalDate date) {
        if (date == null) {
            throw new BuzzException("日期不能为空");
        }
        return date;
    }

    private boolean isWeekday(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }
}
