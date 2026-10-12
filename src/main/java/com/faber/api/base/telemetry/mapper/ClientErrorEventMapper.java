package com.faber.api.base.telemetry.mapper;

import com.faber.api.base.telemetry.entity.ClientErrorEvent;
import com.faber.api.base.telemetry.vo.TelemetryDashboardOverview;
import com.faber.api.base.telemetry.vo.TelemetryDashboardTrend;
import com.faber.api.base.telemetry.vo.TelemetryGlobalDashboardAppRank;
import com.faber.api.base.telemetry.vo.TelemetryGlobalDashboardOverview;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/** 客户端异常事件数据访问。 */
public interface ClientErrorEventMapper extends FaBaseMapper<ClientErrorEvent> {

    TelemetryDashboardOverview selectDashboardErrorOverview(@Param("appId") Long appId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<TelemetryDashboardTrend> selectDashboardErrorTrend(@Param("appId") Long appId, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    TelemetryGlobalDashboardOverview selectGlobalDashboardErrorOverview(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<TelemetryDashboardTrend> selectGlobalDashboardErrorTrend(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<TelemetryGlobalDashboardAppRank> selectGlobalAppErrorRank(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
