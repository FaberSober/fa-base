package com.faber.api.base.telemetry.biz;

import com.faber.api.base.telemetry.mapper.ClientErrorEventMapper;
import com.faber.api.base.telemetry.mapper.StatEventMapper;
import com.faber.api.base.telemetry.mapper.TelemetryAppMapper;
import com.faber.api.base.telemetry.entity.TelemetryApp;
import com.faber.api.base.telemetry.vo.TelemetryDashboardOverview;
import com.faber.api.base.telemetry.vo.TelemetryDashboardRank;
import com.faber.api.base.telemetry.vo.TelemetryDashboardTrend;
import com.faber.api.base.telemetry.vo.TelemetryGlobalDashboardAppRank;
import com.faber.api.base.telemetry.vo.TelemetryGlobalDashboardOverview;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final TelemetryAppMapper telemetryAppMapper;

    public TelemetryDashboardBiz(
            StatEventMapper statEventMapper,
            ClientErrorEventMapper clientErrorEventMapper,
            TelemetryAppMapper telemetryAppMapper
    ) {
        this.statEventMapper = statEventMapper;
        this.clientErrorEventMapper = clientErrorEventMapper;
        this.telemetryAppMapper = telemetryAppMapper;
    }

    public TelemetryDashboardOverview overview(Long appId) {
        LocalDate today = LocalDate.now(ZONE_ID);
        Date startTime = startOfDay(today);
        Date endTime = startOfDay(today.plusDays(1));
        TelemetryDashboardOverview overview = statEventMapper.selectDashboardOverview(appId, startTime, endTime);
        TelemetryDashboardOverview errorOverview = clientErrorEventMapper.selectDashboardErrorOverview(appId, startTime, endTime);
        overview.setErrorCount(errorOverview.getErrorCount());
        overview.setAffectedUserCount(errorOverview.getAffectedUserCount());
        return overview;
    }

    public List<TelemetryDashboardTrend> trend(Long appId, int days) {
        int safeDays = safeDays(days);
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate startDate = today.minusDays(safeDays - 1L);
        Date startTime = startOfDay(startDate);
        Date endTime = startOfDay(today.plusDays(1));
        Map<LocalDate, TelemetryDashboardTrend> trends = new HashMap<>();
        for (TelemetryDashboardTrend trend : statEventMapper.selectDashboardTrend(appId, startTime, endTime)) {
            trends.put(asLocalDate(trend.getStatDate()), trend);
        }
        for (TelemetryDashboardTrend errorTrend : clientErrorEventMapper.selectDashboardErrorTrend(appId, startTime, endTime)) {
            trends.computeIfAbsent(asLocalDate(errorTrend.getStatDate()), this::emptyTrend)
                    .setErrorCount(errorTrend.getErrorCount());
        }

        List<TelemetryDashboardTrend> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            result.add(trends.computeIfAbsent(date, this::emptyTrend));
        }
        return result;
    }

    public List<TelemetryDashboardRank> moduleRank(Long appId) {
        return statEventMapper.selectModuleRank(appId, startOfDay(LocalDate.now(ZONE_ID).minusDays(29)), 10);
    }

    public List<TelemetryDashboardRank> eventRank(Long appId) {
        return statEventMapper.selectEventRank(appId, startOfDay(LocalDate.now(ZONE_ID).minusDays(29)), 10);
    }

    public TelemetryGlobalDashboardOverview globalOverview() {
        LocalDate today = LocalDate.now(ZONE_ID);
        Date startTime = startOfDay(today);
        Date endTime = startOfDay(today.plusDays(1));
        TelemetryGlobalDashboardOverview overview = statEventMapper.selectGlobalDashboardOverview(startTime, endTime);
        TelemetryGlobalDashboardOverview errorOverview = clientErrorEventMapper.selectGlobalDashboardErrorOverview(startTime, endTime);
        List<TelemetryApp> apps = telemetryAppMapper.selectList(null);
        overview.setAppCount((long) apps.size());
        overview.setEnabledAppCount(apps.stream().filter(app -> Boolean.TRUE.equals(app.getEnabled())).count());
        overview.setErrorCount(errorOverview.getErrorCount());
        return overview;
    }

    public List<TelemetryDashboardTrend> globalTrend(int days) {
        int safeDays = safeDays(days);
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate startDate = today.minusDays(safeDays - 1L);
        Date startTime = startOfDay(startDate);
        Date endTime = startOfDay(today.plusDays(1));
        Map<LocalDate, TelemetryDashboardTrend> trends = new HashMap<>();
        for (TelemetryDashboardTrend trend : statEventMapper.selectGlobalDashboardTrend(startTime, endTime)) {
            trends.put(asLocalDate(trend.getStatDate()), trend);
        }
        for (TelemetryDashboardTrend errorTrend : clientErrorEventMapper.selectGlobalDashboardErrorTrend(startTime, endTime)) {
            trends.computeIfAbsent(asLocalDate(errorTrend.getStatDate()), this::emptyTrend)
                    .setErrorCount(errorTrend.getErrorCount());
        }

        List<TelemetryDashboardTrend> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(today); date = date.plusDays(1)) {
            result.add(trends.computeIfAbsent(date, this::emptyTrend));
        }
        return result;
    }

    public List<TelemetryGlobalDashboardAppRank> globalAppRank(int days) {
        int safeDays = safeDays(days);
        LocalDate today = LocalDate.now(ZONE_ID);
        Date startTime = startOfDay(today.minusDays(safeDays - 1L));
        Date endTime = startOfDay(today.plusDays(1));
        Map<Long, TelemetryGlobalDashboardAppRank> ranks = new HashMap<>();
        for (TelemetryApp app : telemetryAppMapper.selectList(null)) {
            TelemetryGlobalDashboardAppRank rank = new TelemetryGlobalDashboardAppRank();
            rank.setAppId(app.getId());
            rank.setAppName(app.getAppName());
            rank.setAppCode(app.getAppCode());
            rank.setClientType(app.getClientType());
            rank.setEnabled(app.getEnabled());
            rank.setActiveUserCount(0L);
            rank.setPageViewCount(0L);
            rank.setBusinessEventCount(0L);
            rank.setErrorCount(0L);
            ranks.put(app.getId(), rank);
        }

        for (TelemetryGlobalDashboardAppRank statRank : statEventMapper.selectGlobalAppStatRank(startTime, endTime)) {
            TelemetryGlobalDashboardAppRank rank = ranks.get(statRank.getAppId());
            if (rank == null) continue;
            rank.setActiveUserCount(statRank.getActiveUserCount());
            rank.setPageViewCount(statRank.getPageViewCount());
            rank.setBusinessEventCount(statRank.getBusinessEventCount());
            rank.setLastReportTime(statRank.getLastReportTime());
        }
        for (TelemetryGlobalDashboardAppRank errorRank : clientErrorEventMapper.selectGlobalAppErrorRank(startTime, endTime)) {
            TelemetryGlobalDashboardAppRank rank = ranks.get(errorRank.getAppId());
            if (rank == null) continue;
            rank.setErrorCount(errorRank.getErrorCount());
            rank.setLastReportTime(maxTime(rank.getLastReportTime(), errorRank.getLastReportTime()));
        }

        List<TelemetryGlobalDashboardAppRank> result = new ArrayList<>(ranks.values());
        result.sort(Comparator.comparing(TelemetryGlobalDashboardAppRank::getPageViewCount, Comparator.reverseOrder())
                .thenComparing(TelemetryGlobalDashboardAppRank::getActiveUserCount, Comparator.reverseOrder())
                .thenComparing(TelemetryGlobalDashboardAppRank::getAppName));
        return result;
    }

    private int safeDays(int days) {
        return Math.max(7, Math.min(days, 30));
    }

    private TelemetryDashboardTrend emptyTrend(LocalDate date) {
        TelemetryDashboardTrend trend = new TelemetryDashboardTrend();
        trend.setStatDate(startOfDay(date));
        trend.setActiveUserCount(0L);
        trend.setLoginCount(0L);
        trend.setPageViewCount(0L);
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

    private Date maxTime(Date left, Date right) {
        if (left == null) return right;
        if (right == null) return left;
        return left.after(right) ? left : right;
    }
}
