package com.faber.api.base.calendar.rest;

import com.faber.api.base.calendar.biz.BaseCalendarDayBiz;
import com.faber.api.base.calendar.entity.BaseCalendarDay;
import com.faber.api.base.calendar.vo.CalendarDayImportPreviewVo;
import com.faber.api.base.calendar.vo.CalendarDayImportReq;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.config.annotation.Permission;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import com.faber.core.web.rest.BaseController;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 统一日历日期维护、年度预览和发布接口。 */
@Permission(permission = "/admin/system/base/calendar")
@FaLogBiz("统一日历日期")
@RestController
@RequestMapping("/api/base/calendar/day")
public class BaseCalendarDayController extends BaseController<BaseCalendarDayBiz, BaseCalendarDay, Long> {

    private final BaseCalendarDayBiz calendarDayBiz;

    public BaseCalendarDayController(BaseCalendarDayBiz calendarDayBiz) {
        this.calendarDayBiz = calendarDayBiz;
    }

    @FaLogOpr("按年查询日历")
    @GetMapping("/year")
    public Ret<List<BaseCalendarDay>> year(@RequestParam String calendarCode,
                                           @RequestParam int year) {
        return ok(calendarDayBiz.listByYear(calendarCode, year));
    }

    @FaLogOpr("预览日历导入")
    @PostMapping("/preview")
    public Ret<CalendarDayImportPreviewVo> preview(@Valid @RequestBody CalendarDayImportReq request) {
        return ok(calendarDayBiz.preview(request));
    }

    @FaLogOpr("发布日历导入")
    @PostMapping("/publish")
    public Ret<CalendarDayImportPreviewVo> publish(@Valid @RequestBody CalendarDayImportReq request) {
        return ok(calendarDayBiz.publish(request));
    }

    @FaLogOpr("批量保存日历")
    @PostMapping("/upsertBatch")
    public Ret<List<BaseCalendarDay>> upsertBatch(@Valid @RequestBody CalendarDayImportReq request) {
        return ok(calendarDayBiz.upsertBatch(request));
    }
}
