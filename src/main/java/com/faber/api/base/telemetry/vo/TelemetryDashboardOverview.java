package com.faber.api.base.telemetry.vo;

import lombok.Data;

/** Telemetry 当日概览指标。 */
@Data
public class TelemetryDashboardOverview {
    private Long activeUserCount;
    private Long loginUserCount;
    private Long pageViewCount;
    private Long businessEventCount;
    private Long errorCount;
    private Long affectedUserCount;
}
