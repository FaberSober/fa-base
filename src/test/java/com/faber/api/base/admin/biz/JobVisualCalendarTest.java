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
    }

    @Test void groupsRealLogsAndExcludesNodes() throws Exception {
        var ds = new JdbcDataSource(); ds.setURL("jdbc:h2:mem:calendar;DB_CLOSE_DELAY=-1");
        try (var connection = ds.getConnection(); var sql = connection.createStatement()) {
            sql.execute("CREATE TABLE base_job_log (begin_time TIMESTAMP, status INT, trigger_source INT)");
            sql.execute("INSERT INTO base_job_log VALUES ('2024-02-01 01:00:00',2,1),('2024-02-01 02:00:00',9,2),('2024-02-01 03:00:00',9,3),('2024-02-02 00:00:00',1,0),('2024-03-01 00:00:00',9,1)");
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
        }
    }
}
