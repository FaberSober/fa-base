package com.faber.api.base.telemetry.mapper;

import com.faber.api.base.telemetry.entity.StatEvent;
import com.faber.api.base.telemetry.vo.TelemetryDashboardOverview;
import com.faber.api.base.telemetry.vo.TelemetryDashboardRank;
import com.faber.api.base.telemetry.vo.TelemetryDashboardTrend;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

public interface StatEventMapper extends FaBaseMapper<StatEvent> {

    @Select("""
            SELECT COUNT(DISTINCT COALESCE(NULLIF(user_id, ''), session_id)) AS active_user_count,
                   COUNT(DISTINCT CASE WHEN event_type = 'LOGIN' AND event_code = 'auth.login.success'
                         THEN COALESCE(NULLIF(user_id, ''), session_id) END) AS login_user_count,
                   COALESCE(SUM(CASE WHEN event_type = 'PAGE_VIEW' THEN 1 ELSE 0 END), 0) AS page_view_count,
                   COALESCE(SUM(CASE WHEN event_type = 'BUSINESS' THEN 1 ELSE 0 END), 0) AS business_event_count
              FROM base_stat_event
             WHERE occur_time >= #{startTime} AND occur_time < #{endTime}
            """)
    TelemetryDashboardOverview selectDashboardOverview(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("""
            SELECT DATE(occur_time) AS stat_date,
                   COUNT(DISTINCT COALESCE(NULLIF(user_id, ''), session_id)) AS active_user_count,
                   COALESCE(SUM(CASE WHEN event_type = 'LOGIN' AND event_code = 'auth.login.success' THEN 1 ELSE 0 END), 0) AS login_count,
                   COALESCE(SUM(CASE WHEN event_type = 'BUSINESS' THEN 1 ELSE 0 END), 0) AS business_event_count
              FROM base_stat_event
             WHERE occur_time >= #{startTime} AND occur_time < #{endTime}
             GROUP BY DATE(occur_time)
             ORDER BY DATE(occur_time)
            """)
    List<TelemetryDashboardTrend> selectDashboardTrend(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    @Select("""
            SELECT COALESCE(NULLIF(module, ''), '其他') AS name,
                   COALESCE(SUM(CASE WHEN event_type = 'PAGE_VIEW' THEN 1 ELSE 0 END), 0) AS primary_count,
                   COALESCE(SUM(CASE WHEN event_type = 'BUSINESS' THEN 1 ELSE 0 END), 0) AS secondary_count
              FROM base_stat_event
             WHERE occur_time >= #{startTime}
             GROUP BY COALESCE(NULLIF(module, ''), '其他')
             ORDER BY primary_count DESC, secondary_count DESC
             LIMIT #{limit}
            """)
    List<TelemetryDashboardRank> selectModuleRank(@Param("startTime") Date startTime, @Param("limit") int limit);

    @Select("""
            SELECT event_code AS name, COUNT(*) AS primary_count, COUNT(DISTINCT COALESCE(NULLIF(user_id, ''), session_id)) AS secondary_count
              FROM base_stat_event
             WHERE occur_time >= #{startTime} AND event_type IN ('ACTION', 'BUSINESS')
             GROUP BY event_code
             ORDER BY primary_count DESC
             LIMIT #{limit}
            """)
    List<TelemetryDashboardRank> selectEventRank(@Param("startTime") Date startTime, @Param("limit") int limit);
}
