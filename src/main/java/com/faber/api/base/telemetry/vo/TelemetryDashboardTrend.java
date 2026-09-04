package com.faber.api.base.telemetry.vo;

import lombok.Data;

import java.util.Date;

/** Telemetry 按日趋势指标。 */
@Data
public class TelemetryDashboardTrend {
    private Date statDate;
    private Long activeUserCount;
    private Long loginCount;
    private Long businessEventCount;
    private Long errorCount;
}
