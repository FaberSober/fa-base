package com.faber.api.base.telemetry.mapper;

import com.faber.api.base.telemetry.entity.ClientErrorEvent;
import com.faber.api.base.telemetry.vo.TelemetryDashboardOverview;
import com.faber.api.base.telemetry.vo.TelemetryDashboardTrend;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/** 客户端异常事件数据访问。 */
public interface ClientErrorEventMapper extends FaBaseMapper<ClientErrorEvent> {

    TelemetryDashboardOverview selectDashboardErrorOverview(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<TelemetryDashboardTrend> selectDashboardErrorTrend(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
