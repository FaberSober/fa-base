package com.faber.api.base.telemetry.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Date;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleTelemetryMapperSqlTest {

    @Test
    void shouldUseOracleCompatibleTelemetrySql() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setDatabaseId("oracle");
        parse(configuration, "mapper/base/telemetry/StatEventMapper.xml");
        parse(configuration, "mapper/base/telemetry/ClientErrorEventMapper.xml");
        parse(configuration, "mapper/base/telemetry/StatDailyMapper.xml");

        assertSqlContains(configuration, "com.faber.api.base.telemetry.mapper.StatEventMapper.selectDashboardTrend", "TRUNC(occur_time)");
        assertSqlContains(configuration, "com.faber.api.base.telemetry.mapper.StatEventMapper.selectModuleRank", "ROWNUM <= ?");
        assertSqlContains(configuration, "com.faber.api.base.telemetry.mapper.StatEventMapper.selectEventRank", "ROWNUM <= ?");
        assertSqlContains(configuration, "com.faber.api.base.telemetry.mapper.ClientErrorEventMapper.selectDashboardErrorTrend", "TRUNC(occur_time)");
        assertSqlContains(configuration, "com.faber.api.base.telemetry.mapper.StatDailyMapper.selectAggregates", "AVG(duration)");
    }

    private void assertSqlContains(MybatisConfiguration configuration, String statementId, String expected) {
        String sql = configuration.getMappedStatement(statementId).getBoundSql(Map.of(
                "startTime", new Date(), "endTime", new Date(), "limit", 10)).getSql();
        assertTrue(sql.contains(expected));
        assertFalse(sql.contains("DATE(occur_time)"));
        assertFalse(sql.contains("LIMIT"));
    }

    private void parse(MybatisConfiguration configuration, String resourcePath) {
        try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
            new XMLMapperBuilder(input, configuration, resourcePath, configuration.getSqlFragments()).parse();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
