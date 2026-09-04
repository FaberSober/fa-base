package com.faber.api.base.telemetry.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.faber.api.base.telemetry.entity.StatDaily;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Date;
import java.util.List;

/** Telemetry 每日聚合数据访问。 */
public interface StatDailyMapper extends BaseMapper<StatDaily> {

    @Select("""
            SELECT app_id, client_type, environment, event_type, event_code, module,
                   COUNT(*) AS pv,
                   COUNT(DISTINCT COALESCE(NULLIF(user_id, ''), session_id)) AS uv,
                   SUM(CASE WHEN result = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
                   SUM(CASE WHEN result = 'FAIL' THEN 1 ELSE 0 END) AS fail_count,
                   AVG(duration) AS avg_duration
              FROM base_stat_event
             WHERE occur_time >= #{startTime} AND occur_time < #{endTime}
             GROUP BY app_id, client_type, environment, event_type, event_code, module
            """)
    List<StatDaily> selectAggregates(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
