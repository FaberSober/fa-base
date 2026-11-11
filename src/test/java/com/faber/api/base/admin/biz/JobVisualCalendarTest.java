package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.mapper.JobLogMapper;
import com.faber.core.exception.BuzzException;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobVisualCalendarTest {
    @Test void validatesMonthAndUsesHalfOpenBounds() {
        var mapper = mock(JobLogMapper.class);
        var biz = new JobBiz();
        ReflectionTestUtils.setField(biz, "jobLogMapper", mapper);
        biz.visualCalendar("2024-02");
        verify(mapper).calendar(LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0));
        assertThrows(BuzzException.class, () -> biz.visualCalendar("2024-13"));
        assertThrows(BuzzException.class, () -> biz.visualCalendar("2024-2"));
        biz.visualTimeline("2024-02-29", null, null, " ");
        verify(mapper).timeline(eq(LocalDateTime.of(2024, 2, 29, 0, 0)), eq(LocalDateTime.of(2024, 3, 1, 0, 0)), isNull(), isNull(), isNull(), eq(List.of()), any());
        biz.visualTimeline("2024-02-29", 1, 9, " 50%_a! ");
        verify(mapper).timelineStats(LocalDateTime.of(2024, 2, 29, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0), 1, 9, "50!%!_a!!");
        assertThrows(BuzzException.class, () -> biz.visualTimeline("2024-02-30", null, null, null));
        assertThrows(BuzzException.class, () -> biz.visualTimeline("2024-02-29", 2, null, null));
        assertThrows(BuzzException.class, () -> biz.visualTimeline("2024-02-29", null, 4, null));
    }

    @Test void groupsRealLogsAndExcludesNodes() throws Exception {
        var ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:calendar;DB_CLOSE_DELAY=-1");
        try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE base_job (id INT, job_name VARCHAR(255), job_type INT, cron VARCHAR(255))");
            sql.execute("CREATE TABLE base_job_log (id INT, job_id INT, job_type INT, job_name VARCHAR(255), clazz_path VARCHAR(255), begin_time TIMESTAMP, end_time TIMESTAMP, duration_ms BIGINT, duration BIGINT, status INT, trigger_source INT)");
            sql.execute("INSERT INTO base_job VALUES (1, '任务', 1, '0 * * * * ?')");
            sql.execute("INSERT INTO base_job_log (id,job_id,begin_time,status,trigger_source) VALUES (1,1,'2024-02-01 01:00:00',2,1),(2,1,'2024-02-01 02:00:00',9,2),(3,1,'2024-02-01 03:00:00',9,3),(4,1,'2024-02-02 00:00:00',1,0),(5,1,'2024-03-01 00:00:00',9,1)");
        }
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        try (var xml = getClass().getClassLoader().getResourceAsStream("mapper/base/admin/JobLogMapper.xml")) {
            assertNotNull(xml);
            new XMLMapperBuilder(xml, config, "mapper/base/admin/JobLogMapper.xml", config.getSqlFragments()).parse();
        }
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            var days = session.getMapper(JobLogMapper.class).calendar(LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0));
            assertEquals(2, days.size());
            assertEquals(LocalDate.of(2024, 2, 1), days.get(0).getDate());
            assertEquals(2, days.get(0).getTotal()); assertEquals(1, days.get(0).getSuccess()); assertEquals(1, days.get(0).getFailed());
            assertEquals(1, days.get(1).getTotal()); assertEquals(0, days.get(1).getSuccess());
            var timeline = session.getMapper(JobLogMapper.class).timeline(LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 2, 2, 0, 0), null, null, null, List.of(), LocalDateTime.of(2024, 2, 2, 12, 0));
            assertEquals(2, timeline.size());
            assertEquals("任务", timeline.get(0).getJobName());
            assertEquals(1, timeline.get(0).getJobId());
            assertEquals(1, timeline.get(0).getJobType());
            assertEquals("0 * * * * ?", timeline.get(0).getCron());
            assertEquals(LocalDateTime.of(2024, 2, 1, 1, 0), timeline.get(0).getBeginTime());
            assertEquals(9, timeline.get(1).getStatus());
        }
    }

    @Test void filtersTimelineAndComputesStats() throws Exception {
        var ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:timelineFilter;DB_CLOSE_DELAY=-1");
        try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE base_job (id INT, job_name VARCHAR(255), job_type INT, cron VARCHAR(255))");
            sql.execute("CREATE TABLE base_job_log (id INT, job_id INT, job_type INT, job_name VARCHAR(255), clazz_path VARCHAR(255), begin_time TIMESTAMP, end_time TIMESTAMP, duration_ms BIGINT, duration BIGINT, status INT, trigger_source INT)");
            sql.execute("INSERT INTO base_job VALUES (1, '同步订单', 0, NULL), (2, '流程50%', 1, NULL)");
            sql.execute("INSERT INTO base_job_log (id,job_id,begin_time,duration_ms,duration,status,trigger_source) VALUES "
                    + "(1,1,'2024-02-01 01:00:00',1000,NULL,2,1),(2,1,'2024-02-01 02:00:00',NULL,3,9,1),(3,1,'2024-02-01 03:00:00',NULL,NULL,1,1),"
                    + "(4,2,'2024-02-01 04:00:00',500,NULL,2,1),(5,2,'2024-02-01 05:00:00',99999,NULL,9,3)");
        }
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        try (var xml = getClass().getClassLoader().getResourceAsStream("mapper/base/admin/JobLogMapper.xml")) {
            new XMLMapperBuilder(xml, config, "mapper/base/admin/JobLogMapper.xml", config.getSqlFragments()).parse();
        }
        var start = LocalDateTime.of(2024, 2, 1, 0, 0); var end = LocalDateTime.of(2024, 2, 2, 0, 0);
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            var mapper = session.getMapper(JobLogMapper.class);
            var all = mapper.timelineStats(start, end, null, null, null);
            assertEquals(4, all.getTotal()); assertEquals(2, all.getSuccess()); assertEquals(1, all.getFailed()); assertEquals(3, all.getFinished());
            assertEquals(1500, all.getAvgDurationMs()); assertEquals(3000, all.getMaxDurationMs());
            assertEquals(List.of(1, 2, 3), mapper.timeline(start, end, 0, null, null, List.of(), end).stream().map(e -> e.getId()).toList());
            assertEquals(List.of(2), mapper.timeline(start, end, null, 9, null, List.of(), end).stream().map(e -> e.getId()).toList());
            assertEquals(List.of(4), mapper.timeline(start, end, null, null, "50!%", List.of(), end).stream().map(e -> e.getId()).toList());
            assertTrue(mapper.timeline(start, end, null, null, "!%订单", List.of(), end).isEmpty());
            var empty = mapper.timelineStats(start, end, 1, 9, null);
            assertEquals(0, empty.getTotal()); assertEquals(0, empty.getSuccess()); assertNull(empty.getAvgDurationMs());
        }
    }

    @Test void validatesWindowAndPassesThreshold() {
        var mapper = mock(JobLogMapper.class);
        var biz = new JobBiz();
        ReflectionTestUtils.setField(biz, "jobLogMapper", mapper);
        biz.visualTimeline("2024-02-29", null, null, null, 3600, 7200);
        var day = LocalDateTime.of(2024, 2, 29, 0, 0);
        verify(mapper).timelineHeavyJobs(day.plusHours(1), day.plusHours(2), null, null, null, 40);
        verify(mapper).timelineStats(day, day.plusDays(1), null, null, null);
        assertThrows(BuzzException.class, () -> biz.visualTimeline("2024-02-29", null, null, null, 7200, 3600));
        assertThrows(BuzzException.class, () -> biz.visualTimeline("2024-02-29", null, null, null, -1, 3600));
        assertThrows(BuzzException.class, () -> biz.visualTimeline("2024-02-29", null, null, null, 0, 86401));
    }

    @Test void marksRecordsCrossingTheSelectedDay() {
        var mapper = mock(JobLogMapper.class);
        var biz = new JobBiz();
        ReflectionTestUtils.setField(biz, "jobLogMapper", mapper);
        var cross = new com.faber.api.base.admin.vo.ret.JobTimelineEntry();
        cross.setBeginTime(LocalDateTime.of(2024, 2, 1, 23, 50));
        cross.setEndTime(LocalDateTime.of(2024, 2, 2, 0, 10));
        cross.setStatus(2);
        when(mapper.timeline(any(), any(), any(), any(), any(), any(), any())).thenReturn(List.of(cross));
        assertTrue(biz.visualTimeline("2024-02-01", null, null, null).entries().get(0).isCrossDay());
        assertTrue(biz.visualTimeline("2024-02-02", null, null, null).entries().get(0).isCrossDay());
    }

    @Test void aggregatesHeavyJobsIntoQuarterHourBuckets() throws Exception {
        var ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:timelineBucket;DB_CLOSE_DELAY=-1");
        try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE base_job (id INT, job_name VARCHAR(255), job_type INT, cron VARCHAR(255))");
            sql.execute("CREATE TABLE base_job_log (id INT, job_id INT, job_type INT, job_name VARCHAR(255), clazz_path VARCHAR(255), begin_time TIMESTAMP, end_time TIMESTAMP, duration_ms BIGINT, duration BIGINT, status INT, trigger_source INT)");
            sql.execute("INSERT INTO base_job VALUES (1, '高频任务', 0, '0 * * * * ?'), (2, '低频任务', 0, NULL)");
            sql.execute("INSERT INTO base_job_log (id,job_id,begin_time,status,trigger_source) "
                    + "SELECT X, 1, DATEADD('MINUTE', X, TIMESTAMP '2024-02-01 01:00:00'), CASE WHEN X = 20 THEN 9 ELSE 2 END, 1 FROM SYSTEM_RANGE(1, 45)");
            sql.execute("INSERT INTO base_job_log (id,job_id,begin_time,status,trigger_source) VALUES (101,2,'2024-02-01 01:10:00',2,1),(102,2,'2024-02-01 05:00:00',2,1),(103,2,'2024-02-01 06:00:00',2,1)");
        }
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        try (var xml = getClass().getClassLoader().getResourceAsStream("mapper/base/admin/JobLogMapper.xml")) {
            new XMLMapperBuilder(xml, config, "mapper/base/admin/JobLogMapper.xml", config.getSqlFragments()).parse();
        }
        var start = LocalDateTime.of(2024, 2, 1, 0, 0); var end = LocalDateTime.of(2024, 2, 2, 0, 0);
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            var mapper = session.getMapper(JobLogMapper.class);
            var heavy = mapper.timelineHeavyJobs(start, end, null, null, null, 40);
            assertEquals(List.of(1), heavy);
            assertTrue(mapper.timelineHeavyJobs(start, end, null, null, null, 45).isEmpty());
            assertEquals(List.of(101, 102, 103), mapper.timeline(start, end, null, null, null, heavy, end).stream().map(e -> e.getId()).toList());
            var buckets = mapper.timelineBuckets(start, end, null, null, null, heavy);
            assertEquals(List.of(4, 5, 6, 7), buckets.stream().map(b -> b.getBucket()).toList());
            assertEquals(List.of(14L, 15L, 15L, 1L), buckets.stream().map(b -> b.getTotal()).toList());
            assertEquals(List.of(0L, 1L, 0L, 0L), buckets.stream().map(b -> b.getFailed()).toList());
            assertEquals("高频任务", buckets.get(0).getJobName());
            assertEquals("0 * * * * ?", buckets.get(0).getCron());
            var narrow = mapper.timelineHeavyJobs(start.plusHours(1), start.plusHours(1).plusMinutes(30), null, null, null, 40);
            assertTrue(narrow.isEmpty());
        }
    }

    @Test void findsCrossDayAndRunningRecordsWithoutLosingThemToAggregation() throws Exception {
        var ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:timelineOverlap;DB_CLOSE_DELAY=-1");
        try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE base_job (id INT, job_name VARCHAR(255), job_type INT, cron VARCHAR(255))");
            sql.execute("CREATE TABLE base_job_log (id INT, job_id INT, job_type INT, job_name VARCHAR(255), clazz_path VARCHAR(255), begin_time TIMESTAMP, end_time TIMESTAMP, duration_ms BIGINT, duration BIGINT, status INT, trigger_source INT)");
            sql.execute("INSERT INTO base_job VALUES (1, '任务', 0, NULL)");
            sql.execute("INSERT INTO base_job_log (id,job_id,begin_time,end_time,status,trigger_source) VALUES "
                    + "(100,1,'2024-02-01 23:50:00','2024-02-02 00:10:00',2,1),"
                    + "(101,1,'2024-02-01 23:45:00','2024-02-02 00:00:00',2,1),"
                    + "(102,1,'2024-02-01 23:40:00',NULL,1,1),"
                    + "(103,1,'2024-02-01 23:30:00',NULL,3,1),"
                    + "(104,1,'2024-02-01 23:20:00',NULL,8,1),"
                    + "(105,1,'2024-02-02 12:00:00',NULL,1,1)");
            sql.execute("INSERT INTO base_job_log (id,job_id,begin_time,status,trigger_source) "
                    + "SELECT X, 1, DATEADD('MINUTE', X, TIMESTAMP '2024-02-02 01:00:00'), 2, 1 FROM SYSTEM_RANGE(1, 41)");
        }
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        try (var xml = getClass().getClassLoader().getResourceAsStream("mapper/base/admin/JobLogMapper.xml")) {
            new XMLMapperBuilder(xml, config, "mapper/base/admin/JobLogMapper.xml", config.getSqlFragments()).parse();
        }
        var first = LocalDateTime.of(2024, 2, 1, 0, 0); var second = first.plusDays(1); var third = second.plusDays(1);
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            var mapper = session.getMapper(JobLogMapper.class);
            assertTrue(mapper.timeline(first, second, null, null, null, List.of(), second.plusHours(13)).stream().anyMatch(e -> e.getId() == 100));
            var heavy = mapper.timelineHeavyJobs(second, third, null, null, null, 40);
            assertEquals(List.of(1), heavy);
            assertEquals(List.of(103, 102, 100, 105), mapper.timeline(second, third, null, null, null, heavy, second.plusHours(13)).stream().map(e -> e.getId()).toList());
            assertEquals(41, mapper.timelineBuckets(second, third, null, null, null, heavy).stream().mapToLong(b -> b.getTotal()).sum());
            assertTrue(mapper.timeline(third, third.plusDays(1), null, null, null, List.of(), second.plusHours(13)).isEmpty());
        }
    }

    @Test void aggregatesTenThousandLogsWithoutLosingCounts() throws Exception {
        var ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:timelineTenThousand;DB_CLOSE_DELAY=-1");
        try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE base_job (id INT, job_name VARCHAR(255), job_type INT, cron VARCHAR(255))");
            sql.execute("CREATE TABLE base_job_log (id INT, job_id INT, job_type INT, job_name VARCHAR(255), clazz_path VARCHAR(255), begin_time TIMESTAMP, end_time TIMESTAMP, duration_ms BIGINT, duration BIGINT, status INT, trigger_source INT)");
            sql.execute("INSERT INTO base_job SELECT X, CONCAT('任务', X), 0, NULL FROM SYSTEM_RANGE(1, 100)");
            sql.execute("INSERT INTO base_job_log (id,job_id,begin_time,status,trigger_source) SELECT X, MOD(X, 100) + 1, DATEADD('SECOND', X, TIMESTAMP '2024-02-01 00:00:00'), 2, 1 FROM SYSTEM_RANGE(1, 10000)");
        }
        var config = new Configuration(new Environment("test", new JdbcTransactionFactory(), ds));
        try (var xml = getClass().getClassLoader().getResourceAsStream("mapper/base/admin/JobLogMapper.xml")) {
            new XMLMapperBuilder(xml, config, "mapper/base/admin/JobLogMapper.xml", config.getSqlFragments()).parse();
        }
        var start = LocalDateTime.of(2024, 2, 1, 0, 0); var end = start.plusDays(1);
        try (var session = new SqlSessionFactoryBuilder().build(config).openSession()) {
            var mapper = session.getMapper(JobLogMapper.class);
            var heavy = mapper.timelineHeavyJobs(start, end, null, null, null, 40);
            assertEquals(100, heavy.size());
            assertEquals(10000, mapper.timelineBuckets(start, end, null, null, null, heavy).stream().mapToLong(b -> b.getTotal()).sum());
            assertEquals(10000, mapper.timelineStats(start, end, null, null, null).getTotal());
            assertTrue(mapper.timeline(start, end, null, null, null, heavy, end).isEmpty());
        }
    }
}
