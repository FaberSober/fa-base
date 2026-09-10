package com.faber.api.base.telemetry.mapper;

import com.faber.api.base.telemetry.entity.StatEvent;
import com.faber.api.base.telemetry.vo.TelemetryDashboardOverview;
import com.faber.api.base.telemetry.vo.TelemetryDashboardRank;
import com.faber.api.base.telemetry.vo.TelemetryDashboardTrend;
import com.faber.api.base.telemetry.vo.TelemetryGlobalDashboardAppRank;
import com.faber.api.base.telemetry.vo.TelemetryGlobalDashboardOverview;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface StatEventMapper extends FaBaseMapper<StatEvent> {

    TelemetryDashboardOverview selectDashboardOverview(@Param("appId") Long appId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<TelemetryDashboardTrend> selectDashboardTrend(@Param("appId") Long appId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<TelemetryDashboardRank> selectModuleRank(@Param("appId") Long appId, @Param("startTime") Date startTime, @Param("limit") int limit);

    List<TelemetryDashboardRank> selectEventRank(@Param("appId") Long appId, @Param("startTime") Date startTime, @Param("limit") int limit);

    TelemetryGlobalDashboardOverview selectGlobalDashboardOverview(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<TelemetryDashboardTrend> selectGlobalDashboardTrend(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<TelemetryGlobalDashboardAppRank> selectGlobalAppStatRank(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
