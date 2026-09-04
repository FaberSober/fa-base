package com.faber.api.base.telemetry.vo;

import lombok.Data;

/** Telemetry 模块或功能排行项。 */
@Data
public class TelemetryDashboardRank {
    private String name;
    private Long primaryCount;
    private Long secondaryCount;
}
