package com.faber.api.base.telemetry.rest;

import com.faber.api.base.telemetry.biz.TelemetryDashboardBiz;
import com.faber.api.base.telemetry.vo.TelemetryDashboardOverview;
import com.faber.api.base.telemetry.vo.TelemetryDashboardRank;
import com.faber.api.base.telemetry.vo.TelemetryDashboardTrend;
import com.faber.core.annotation.FaLogBiz;
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

    @GetMapping("/overview")
    public Ret<TelemetryDashboardOverview> overview() {
        return ok(telemetryDashboardBiz.overview());
    }

    @GetMapping("/trend")
    public Ret<List<TelemetryDashboardTrend>> trend(@RequestParam(defaultValue = "7") int days) {
        return ok(telemetryDashboardBiz.trend(days));
    }

    @GetMapping("/moduleRank")
    public Ret<List<TelemetryDashboardRank>> moduleRank() {
        return ok(telemetryDashboardBiz.moduleRank());
    }

    @GetMapping("/eventRank")
    public Ret<List<TelemetryDashboardRank>> eventRank() {
        return ok(telemetryDashboardBiz.eventRank());
    }
}
