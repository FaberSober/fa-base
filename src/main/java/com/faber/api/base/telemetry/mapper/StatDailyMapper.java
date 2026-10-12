package com.faber.api.base.telemetry.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.faber.api.base.telemetry.entity.StatDaily;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/** Telemetry 每日聚合数据访问。 */
public interface StatDailyMapper extends BaseMapper<StatDaily> {

    List<StatDaily> selectAggregates(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
