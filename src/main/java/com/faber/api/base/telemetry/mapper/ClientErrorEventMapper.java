package com.faber.api.base.telemetry.mapper;

import com.faber.api.base.telemetry.entity.ClientErrorEvent;
import com.faber.api.base.telemetry.vo.TelemetryDashboardOverview;
import com.faber.api.base.telemetry.vo.TelemetryDashboardTrend;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/** 客户端异常事件数据访问。 */
public interface ClientErrorEventMapper extends FaBaseMapper<ClientErrorEvent> {

    @Select("""
            SELECT COUNT(*) AS error_count,
                   COUNT(DISTINCT COALESCE(NULLIF(user_id, ''), session_id)) AS affected_user_count
              FROM base_client_error_event
             WHERE occur_time >= #{startTime} AND occur_time < #{endTime}
            """)
    TelemetryDashboardOverview selectDashboardErrorOverview(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("""
            SELECT DATE(occur_time) AS stat_date, COUNT(*) AS error_count
              FROM base_client_error_event
             WHERE occur_time >= #{startTime} AND occur_time < #{endTime}
             GROUP BY DATE(occur_time)
             ORDER BY DATE(occur_time)
            """)
    List<TelemetryDashboardTrend> selectDashboardErrorTrend(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
