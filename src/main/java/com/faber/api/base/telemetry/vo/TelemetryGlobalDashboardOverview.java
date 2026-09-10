package com.faber.api.base.telemetry.vo;

import lombok.Data;

/** Telemetry 全局当日概览指标。 */
@Data
public class TelemetryGlobalDashboardOverview {
    private Long appCount;
    private Long enabledAppCount;
    private Long activeUserCount;
    private Long pageViewCount;
    private Long businessEventCount;
    private Long errorCount;
}
