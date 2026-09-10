package com.faber.api.base.telemetry.vo;

import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import lombok.Data;

import java.util.Date;

/** Telemetry 全局概览中的应用统计项。 */
@Data
public class TelemetryGlobalDashboardAppRank {
    private Long appId;
    private String appName;
    private String appCode;
    private TelemetryClientTypeEnum clientType;
    private Boolean enabled;
    private Long activeUserCount;
    private Long pageViewCount;
    private Long businessEventCount;
    private Long errorCount;
    private Date lastReportTime;
}
