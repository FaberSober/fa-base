package com.faber.api.base.telemetry.rest;

import com.faber.api.base.telemetry.biz.TelemetryDashboardBiz;
import com.faber.api.base.telemetry.vo.TelemetryDashboardOverview;
import com.faber.api.base.telemetry.vo.TelemetryDashboardRank;
import com.faber.api.base.telemetry.vo.TelemetryDashboardTrend;
import com.faber.api.base.telemetry.vo.TelemetryGlobalDashboardAppRank;
import com.faber.api.base.telemetry.vo.TelemetryGlobalDashboardOverview;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Telemetry 统计概览。 */
@FaLogBiz("Telemetry 概览")
@RestController
@RequestMapping("/api/base/telemetry/dashboard")
public class TelemetryDashboardController extends BaseResHandler {

    private final TelemetryDashboardBiz telemetryDashboardBiz;

    public TelemetryDashboardController(TelemetryDashboardBiz telemetryDashboardBiz) {
        this.telemetryDashboardBiz = telemetryDashboardBiz;
    }

    @FaLogOpr(value = "查询应用遥测概览", crud = LogCrudEnum.R)
    @GetMapping("/overview")
    public Ret<TelemetryDashboardOverview> overview(@RequestParam Long appId) {
        return ok(telemetryDashboardBiz.overview(appId));
    }

    @FaLogOpr(value = "查询应用遥测趋势", crud = LogCrudEnum.R)
    @GetMapping("/trend")
    public Ret<List<TelemetryDashboardTrend>> trend(@RequestParam Long appId, @RequestParam(defaultValue = "7") int days) {
        return ok(telemetryDashboardBiz.trend(appId, days));
    }

    @FaLogOpr(value = "查询应用模块排行", crud = LogCrudEnum.R)
    @GetMapping("/moduleRank")
    public Ret<List<TelemetryDashboardRank>> moduleRank(@RequestParam Long appId) {
        return ok(telemetryDashboardBiz.moduleRank(appId));
    }

    @FaLogOpr(value = "查询应用事件排行", crud = LogCrudEnum.R)
    @GetMapping("/eventRank")
    public Ret<List<TelemetryDashboardRank>> eventRank(@RequestParam Long appId) {
        return ok(telemetryDashboardBiz.eventRank(appId));
    }

    @FaLogOpr(value = "查询全局遥测概览", crud = LogCrudEnum.R)
    @GetMapping("/globalOverview")
    public Ret<TelemetryGlobalDashboardOverview> globalOverview() {
        return ok(telemetryDashboardBiz.globalOverview());
    }

    @FaLogOpr(value = "查询全局遥测趋势", crud = LogCrudEnum.R)
    @GetMapping("/globalTrend")
    public Ret<List<TelemetryDashboardTrend>> globalTrend(@RequestParam(defaultValue = "7") int days) {
        return ok(telemetryDashboardBiz.globalTrend(days));
    }

    @FaLogOpr(value = "查询全局应用排行", crud = LogCrudEnum.R)
    @GetMapping("/globalAppRank")
    public Ret<List<TelemetryGlobalDashboardAppRank>> globalAppRank(@RequestParam(defaultValue = "7") int days) {
        return ok(telemetryDashboardBiz.globalAppRank(days));
    }
}
