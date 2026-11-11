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
        verify(mapper).timeline(LocalDateTime.of(2024, 2, 29, 0, 0), LocalDateTime.of(2024, 3, 1, 0, 0), null, null, null);
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
            var timeline = session.getMapper(JobLogMapper.class).timeline(LocalDateTime.of(2024, 2, 1, 0, 0), LocalDateTime.of(2024, 2, 2, 0, 0), null, null, null);
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
            assertEquals(List.of(1, 2, 3), mapper.timeline(start, end, 0, null, null).stream().map(e -> e.getId()).toList());
            assertEquals(List.of(2), mapper.timeline(start, end, null, 9, null).stream().map(e -> e.getId()).toList());
            assertEquals(List.of(4), mapper.timeline(start, end, null, null, "50!%").stream().map(e -> e.getId()).toList());
            assertTrue(mapper.timeline(start, end, null, null, "!%订单").isEmpty());
            var empty = mapper.timelineStats(start, end, 1, 9, null);
            assertEquals(0, empty.getTotal()); assertEquals(0, empty.getSuccess()); assertNull(empty.getAvgDurationMs());
        }
    }
}
