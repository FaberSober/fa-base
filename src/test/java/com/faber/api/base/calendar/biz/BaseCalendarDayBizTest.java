package com.faber.api.base.calendar.biz;

import com.faber.api.base.calendar.entity.BaseCalendar;
import com.faber.api.base.calendar.entity.BaseCalendarDay;
import com.faber.api.base.calendar.enums.BaseCalendarDayTypeEnum;
import com.faber.api.base.calendar.mapper.BaseCalendarMapper;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseCalendarDayBizTest {

    @Test
    void rejectsInconsistentHolidayState() {
        BaseCalendarMapper mapper = enabledCalendarMapper();
        BaseCalendarDayBiz biz = new BaseCalendarDayBiz(mapper);
        BaseCalendarDay day = day(BaseCalendarDayTypeEnum.HOLIDAY, true);

        assertThrows(BuzzException.class, () -> biz.saveBefore(day));
    }

    @Test
    void acceptsMakeupWorkdayOnWeekend() {
        BaseCalendarMapper mapper = enabledCalendarMapper();
        BaseCalendarDayBiz biz = new BaseCalendarDayBiz(mapper);
        BaseCalendarDay day = day(BaseCalendarDayTypeEnum.MAKEUP_WORKDAY, true);

        assertDoesNotThrow(() -> biz.saveBefore(day));
    }

    private BaseCalendarMapper enabledCalendarMapper() {
        BaseCalendarMapper mapper = mock(BaseCalendarMapper.class);
        BaseCalendar calendar = new BaseCalendar();
        calendar.setCalendarCode("CN_OA");
        calendar.setEnabled(true);
        when(mapper.selectEnabledByCode(eq("CN_OA"), eq(true), eq(false))).thenReturn(calendar);
        return mapper;
    }

    private BaseCalendarDay day(BaseCalendarDayTypeEnum type, boolean open) {
        BaseCalendarDay day = new BaseCalendarDay();
        day.setCalendarCode("cn_oa");
        day.setCalendarDate(LocalDate.of(2026, 9, 13));
        day.setDayType(type);
        day.setIsOpen(open);
        return day;
    }
}
