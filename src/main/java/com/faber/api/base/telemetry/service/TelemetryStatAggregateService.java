package com.faber.api.base.telemetry.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.faber.api.base.telemetry.entity.StatDaily;
import com.faber.api.base.telemetry.mapper.StatDailyMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/** 将原始统计事件汇总为按日数据；可重跑指定日期以修正迟到事件。 */
@Service
public class TelemetryStatAggregateService {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final StatDailyMapper statDailyMapper;

    public TelemetryStatAggregateService(StatDailyMapper statDailyMapper) {
        this.statDailyMapper = statDailyMapper;
    }

    /** 每日凌晨聚合前一日。 */
    @Scheduled(cron = "0 10 0 * * ?")
    @Transactional(rollbackFor = Exception.class)
    public void aggregatePreviousDay() {
        aggregate(LocalDate.now(ZONE_ID).minusDays(1));
    }

    @Transactional(rollbackFor = Exception.class)
    public void aggregate(LocalDate statDate) {
        Date startTime = Date.from(statDate.atStartOfDay(ZONE_ID).toInstant());
        Date endTime = Date.from(statDate.plusDays(1).atStartOfDay(ZONE_ID).toInstant());
        List<StatDaily> aggregates = statDailyMapper.selectAggregates(startTime, endTime);

        statDailyMapper.delete(new LambdaQueryWrapper<StatDaily>()
                .eq(StatDaily::getStatDate, startTime));

        Date now = new Date();
        for (StatDaily daily : aggregates) {
            daily.setStatDate(startTime);
            daily.setCreateTime(now);
            daily.setUpdateTime(now);
            statDailyMapper.insert(daily);
        }
    }
}
