package com.faber.api.base.telemetry.biz;

import com.faber.api.base.telemetry.mapper.ClientErrorEventMapper;
import com.faber.api.base.telemetry.mapper.StatEventMapper;
import com.faber.api.base.telemetry.vo.TelemetryDashboardOverview;
import com.faber.api.base.telemetry.vo.TelemetryDashboardRank;
import com.faber.api.base.telemetry.vo.TelemetryDashboardTrend;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Telemetry Dashboard 查询；近期范围有限，今日直接读原始事件以保证实时性。 */
@Service
public class TelemetryDashboardBiz {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final StatEventMapper statEventMapper;
    private final ClientErrorEventMapper clientErrorEventMapper;

    public TelemetryDashboardBiz(StatEventMapper statEventMapper, ClientErrorEventMapper clientErrorEventMapper) {
        this.statEventMapper = statEventMapper;
        this.clientErrorEventMapper = clientErrorEventMapper;
    }

    public TelemetryDashboardOverview overview() {
        LocalDate today = LocalDate.now(ZONE_ID);
        Date startTime = startOfDay(today);
        Date endTime = startOfDay(today.plusDays(1));
        TelemetryDashboardOverview overview = statEventMapper.selectDashboardOverview(startTime, endTime);
        TelemetryDashboardOverview errorOverview = clientErrorEventMapper.selectDashboardErrorOverview(startTime, endTime);
        overview.setErrorCount(errorOverview.getErrorCount());
        overview.setAffectedUserCount(errorOverview.getAffectedUserCount());
        return overview;
    }

    public List<TelemetryDashboardTrend> trend(int days) {
        int safeDays = Math.max(7, Math.min(days, 30));
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate startDate = today.minusDays(safeDays - 1L);
        Date startTime = startOfDay(startDate);
        Date endTime = startOfDay(today.plusDays(1));
        Map<LocalDate, TelemetryDashboardTrend> trends = new HashMap<>();
        for (TelemetryDashboardTrend trend : statEventMapper.selectDashboardTrend(startTime, endTime)) {
            trends.put(asLocalDate(trend.getStatDate()), trend);
        }
        for (TelemetryDashboardTrend errorTrend : clientErrorEventMapper.selectDashboardErrorTrend(startTime, endTime)) {
            trends.computeIfAbsent(asLocalDate(errorTrend.getStatDate()), this::emptyTrend)
                    .setErrorCount(errorTrend.getErrorCount());
        }

        List<TelemetryDashboardTrend> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            result.add(trends.computeIfAbsent(date, this::emptyTrend));
        }
        return result;
    }

    public List<TelemetryDashboardRank> moduleRank() {
        return statEventMapper.selectModuleRank(startOfDay(LocalDate.now(ZONE_ID).minusDays(29)), 10);
    }

    public List<TelemetryDashboardRank> eventRank() {
        return statEventMapper.selectEventRank(startOfDay(LocalDate.now(ZONE_ID).minusDays(29)), 10);
    }

    private TelemetryDashboardTrend emptyTrend(LocalDate date) {
        TelemetryDashboardTrend trend = new TelemetryDashboardTrend();
        trend.setStatDate(startOfDay(date));
        trend.setActiveUserCount(0L);
        trend.setLoginCount(0L);
        trend.setBusinessEventCount(0L);
        trend.setErrorCount(0L);
        return trend;
    }

    private Date startOfDay(LocalDate date) {
        return Date.from(date.atStartOfDay(ZONE_ID).toInstant());
    }

    private LocalDate asLocalDate(Date date) {
        return date.toInstant().atZone(ZONE_ID).toLocalDate();
    }
}
