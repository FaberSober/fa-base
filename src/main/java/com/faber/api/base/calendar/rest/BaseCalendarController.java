package com.faber.api.base.calendar.rest;

import com.faber.api.base.calendar.biz.BaseCalendarBiz;
import com.faber.api.base.calendar.entity.BaseCalendar;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.config.annotation.Permission;
import com.faber.core.web.rest.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 统一日历定义管理接口。 */
@Permission(permission = "/admin/system/base/calendar")
@FaLogBiz("统一日历定义")
@RestController
@RequestMapping("/api/base/calendar/definition")
public class BaseCalendarController extends BaseController<BaseCalendarBiz, BaseCalendar, Long> {
}
