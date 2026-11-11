package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.mapper.JobLogMapper;
import com.faber.api.base.admin.mapper.JobMapper;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.Test;
import org.quartz.CronExpression;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;

import java.time.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobVisualPlanTest {
    private Job job(String cron, boolean enabled) {
        var job = new Job(); job.setId(7); job.setJobName("计划任务"); job.setJobType(0); job.setCron(cron); job.setStatus(enabled); return job;
    }

    @Test void cronUsesDayBoundariesAndExcludesElapsedTimes() {
        var zone = ZoneId.of("UTC"); var day = LocalDate.of(2026, 11, 11);
        var now = Instant.parse("2026-11-11T10:30:00Z");
        var points = JobVisualPlanService.points(job("0 0 * * * ?", true), day, now, zone);
        assertEquals(13, points.size()); assertEquals(11 * 3600, points.get(0)); assertEquals(23 * 3600, points.get(12));
        assertEquals(0, JobVisualPlanService.points(job("invalid", true), day, now, zone).size());
        assertEquals(24, JobVisualPlanService.points(job("0 0 * * * ?", true), day.plusDays(1), now, zone).size());
        assertTrue(JobVisualPlanService.points(job("0 0 * * * ?", true), day.minusDays(1), now, zone).isEmpty());
    }

    @Test void denseCronMatchesQuartzAcrossTimeZonesAndDaylightTransitions() throws Exception {
        for (var zone : List.of(ZoneId.of("UTC"), ZoneId.of("America/New_York"))) {
            for (var day : List.of(LocalDate.of(2026, 3, 8), LocalDate.of(2026, 11, 1))) {
                var now = day.atTime(1, 15, 12).atZone(zone).toInstant().plusMillis(500);
                for (var expression : List.of("* * * * * ?", "3/7 * * * * ?", "1,12,30-50/4 * * * * ?", "58-2 * * * * ?", "0 0/5 1-4 * * ?")) {
                    var cron = new CronExpression(expression); cron.setTimeZone(TimeZone.getTimeZone(zone));
                    var end = day.plusDays(1).atStartOfDay(zone).toInstant();
                    var expected = new ArrayList<Integer>();
                    for (var next = cron.getTimeAfter(Date.from(now)); next != null && next.toInstant().isBefore(end); next = cron.getTimeAfter(next))
                        expected.add(next.toInstant().atZone(zone).toLocalTime().toSecondOfDay());
                    assertEquals(expected, JobVisualPlanService.points(job(expression, true), day, now, zone), expression + " " + zone + " " + day);
                }
            }
        }
    }

    @Test void excludesPausedJobsAndAggregatesHighFrequencyPlans() {
        var mapper = mock(JobMapper.class); var logs = mock(JobLogMapper.class);
        var service = new JobVisualPlanService(mapper, logs);
        var paused = job("* * * * * ?", false); paused.setId(8);
        when(mapper.selectList(isNull())).thenReturn(List.of(job("* * * * * ?", true), paused));
        when(logs.recentSuccessAverage(7)).thenReturn(1234L);
        var day = LocalDate.now().plusDays(1);
        var result = service.plan(day.toString(), null, null).items();
        assertEquals(96, result.size());
        assertEquals(86400, result.stream().mapToInt(item -> item.count()).sum());
        assertFalse(result.get(0).paused()); assertEquals(1234L, result.get(0).durationMs());
        assertTrue(result.stream().allMatch(item -> item.jobId() == 7));
        assertTrue(service.calendar(day.toString().substring(0, 7)).contains(day));
        assertTrue(service.plan(day.toString(), 1, null).items().isEmpty());
        when(mapper.selectList(isNull())).thenReturn(List.of(paused));
        assertTrue(service.plan(day.toString(), null, null).items().isEmpty());
        assertTrue(service.calendar(day.toString().substring(0, 7)).isEmpty());
        assertThrows(BuzzException.class, () -> service.plan("2026-02-30", null, null));
    }

    @Test void durationUsesOnlyTheLatestTwentySuccessfulExecutions() throws Exception {
        var ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:planDuration;DB_CLOSE_DELAY=-1");
        try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE base_job_log (id INT, job_id INT, begin_time TIMESTAMP, status INT, duration_ms BIGINT, duration BIGINT)");
            sql.execute("INSERT INTO base_job_log SELECT X, 7, DATEADD('MINUTE', X, TIMESTAMP '2026-11-11 00:00:00'), 2, X * 1000, NULL FROM SYSTEM_RANGE(1, 25)");
            sql.execute("INSERT INTO base_job_log VALUES (99, 7, TIMESTAMP '2026-11-12 00:00:00', 9, 999999, NULL)");
        }
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        try (var xml = getClass().getClassLoader().getResourceAsStream("mapper/base/admin/JobLogMapper.xml")) {
            new XMLMapperBuilder(xml, config, "mapper/base/admin/JobLogMapper.xml", config.getSqlFragments()).parse();
        }
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            assertEquals(15500L, session.getMapper(JobLogMapper.class).recentSuccessAverage(7));
            assertNull(session.getMapper(JobLogMapper.class).recentSuccessAverage(8));
            try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
                sql.execute("UPDATE base_job_log SET duration_ms = NULL WHERE id = 25");
            }
            session.clearCache();
            assertEquals(15000L, session.getMapper(JobLogMapper.class).recentSuccessAverage(7));
        }
    }
}
