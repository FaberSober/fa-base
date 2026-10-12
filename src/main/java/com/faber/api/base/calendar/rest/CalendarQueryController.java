package com.faber.api.base.calendar.rest;

import com.faber.api.base.calendar.service.CalendarService;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** 统一日历只读查询接口。维护和年度导入接口在 Sprint 2 增加。 */
@FaLogBiz("统一日历查询")
@RestController
@RequestMapping("/api/base/calendar")
public class CalendarQueryController extends BaseResHandler {

    private final CalendarService calendarService;

    public CalendarQueryController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping("/isOpen")
    public Ret<Boolean> isOpen(@RequestParam String calendarCode,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ok(calendarService.isOpen(calendarCode, date));
    }

    @GetMapping("/isTradingDay")
    public Ret<Boolean> isTradingDay(@RequestParam String market,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ok(calendarService.isTradingDay(market, date));
    }

    @GetMapping("/previousOrSameOpenDate")
    public Ret<LocalDate> previousOrSameOpenDate(
            @RequestParam String calendarCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ok(calendarService.previousOrSameOpenDate(calendarCode, date));
    }
}
