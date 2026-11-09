package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.flow.JobFlowDefinitionValidator;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobFlowDefinitionTest {
    private static final String FLOW = """
      {"schemaVersion":1,"name":"报表流程","maxConcurrency":2,"nodes":[
      {"id":"a","name":"任务","clazzPath":"com.example.Job","position":{"x":0,"y":0}},
      {"id":"b","name":"重复任务","clazzPath":"com.example.Job","position":{"x":0,"y":100}}],
      "edges":[{"source":"a","target":"b"}]}
      """;
    @Test void validatesRegisteredClassesAndDag() {
        assertDoesNotThrow(() -> JobFlowDefinitionValidator.validate(FLOW, Set.of("com.example.Job")));
        assertThrows(BuzzException.class, () -> JobFlowDefinitionValidator.validate(FLOW, Set.of()));
        assertThrows(BuzzException.class, () -> JobFlowDefinitionValidator.validate(FLOW.replace("\"target\":\"b\"", "\"target\":\"a\""), Set.of("com.example.Job")));
        assertThrows(BuzzException.class, () -> JobFlowDefinitionValidator.validate(FLOW.replace("\"id\":\"b\"", "\"id\":\"a\""), Set.of("com.example.Job")));
        assertThrows(BuzzException.class, () -> JobFlowDefinitionValidator.validate(FLOW.replace("\"maxConcurrency\":2", "\"maxConcurrency\":21"), Set.of("com.example.Job")));
        assertThrows(BuzzException.class, () -> JobFlowDefinitionValidator.validate("{}", Set.of()));
        assertThrows(BuzzException.class, () -> JobFlowDefinitionValidator.validate(FLOW.replace("\"maxConcurrency\":2", "\"maxConcurrency\":4294967298"), Set.of("com.example.Job")));
        assertThrows(BuzzException.class, () -> JobFlowDefinitionValidator.validate(FLOW.replace("[{\"source\":\"a\",\"target\":\"b\"}]", "[{\"source\":\"a\",\"target\":\"b\"},{\"source\":\"b\",\"target\":\"a\"}]"), Set.of("com.example.Job")));
        assertThrows(BuzzException.class, () -> JobFlowDefinitionValidator.validate(" ".repeat(262145), Set.of()));
    }
    @Test void forbidsTypeSwitchAndFlowExecution() {
        JobBiz biz = spy(new JobBiz());
        Job existing = new Job(); existing.setId(10); existing.setJobType(0);
        doReturn(existing).when(biz).getById(10);
        Job update = new Job(); update.setId(10); update.setJobType(1);
        assertThrows(BuzzException.class, () -> biz.saveBefore(update));
        existing.setJobType(1);

        doReturn(existing).when(biz).getById(10L);
        assertThrows(BuzzException.class, () -> biz.runOneTime(10L));
    }
    @Test void flowSaveNormalizesClassAndRejectsEnabledStatus() {
        JobBiz biz = new JobBiz();
        Job flow = new Job(); flow.setJobName("流程"); flow.setJobType(1);
        flow.setCron("0 * * * * ?");
        flow.setFlowDefinition(FLOW.replace("com.example.Job", "com.faber.config.quartz.customer.JobDemo1"));
        assertDoesNotThrow(() -> biz.saveBefore(flow));
        assertEquals("", flow.getClazzPath());
        assertFalse(flow.getStatus());
        flow.setStatus(true);
        assertThrows(BuzzException.class, () -> biz.saveBefore(flow));
    }
    @Test void enabledFlowCanSaveDefinitionWithoutChangingScheduleState() {
        JobBiz biz = spy(new JobBiz());
        Job existing = new Job(); existing.setId(10); existing.setJobType(1); existing.setStatus(true);
        existing.setCron("0 * * * * ?"); existing.setJobName("流程");
        existing.setFlowDefinition(FLOW.replace("com.example.Job", "com.faber.config.quartz.customer.JobDemo1"));
        doReturn(existing).when(biz).getById(10);
        Job patch = new Job(); patch.setId(10);
        assertDoesNotThrow(() -> biz.saveBefore(patch)); assertTrue(patch.getStatus());
        patch.setStatus(false); assertThrows(BuzzException.class, () -> biz.saveBefore(patch));
        patch.setStatus(null); patch.setCron("0/10 * * * * ?");
        assertThrows(BuzzException.class, () -> biz.saveBefore(patch));
    }
    @Test void migrationUsesNativeDialectAndProductionSplitter() throws Exception {
        for (String dialect : Set.of("mysql", "postgre")) {
            String sql;
            try (var input = new ClassPathResource("sql/fa-base/" + dialect + "/1.0.54_base_job_flow_definition.sql").getInputStream()) {
                sql = new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            }
            var dbType = dialect.equals("mysql") ? com.alibaba.druid.DbType.mysql : com.alibaba.druid.DbType.postgresql;
            var parsed = com.alibaba.druid.sql.SQLUtils.parseStatements(druidInput(sql, dialect), dbType);
            java.sql.Connection connection = mock(java.sql.Connection.class);
            java.sql.Statement statement = mock(java.sql.Statement.class);
            when(connection.createStatement()).thenReturn(statement);
            com.faber.core.utils.SqlUtils.executeSql(connection, sql);
            var captured = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(statement, times(parsed.size())).execute(captured.capture());
            for (String part : captured.getAllValues()) assertEquals(1, com.alibaba.druid.sql.SQLUtils.parseStatements(druidInput(part, dialect), dbType).size());
        }
    }
    // 当前 Druid 版本无法解析 PostgreSQL identity 子句；仅语法检查时剔除该子句。
    // 实际 SqlUtils 分句与 execute 捕获始终使用原始 SQL，不以此替代实库迁移验收。
    private static String druidInput(String sql, String dialect) {
        return dialect.equals("postgre") ? sql.replace("GENERATED BY DEFAULT AS IDENTITY", "") : sql;
    }
    @Test void migrationHeadersAndVersionsPassProductionParser() throws Exception {
        SystemUpdateLogBiz parser = new SystemUpdateLogBiz();
        for (String dialect : Set.of("mysql", "postgre")) {
            var resource = new ClassPathResource("sql/fa-base/" + dialect + "/1.0.54_base_job_flow_definition.sql");
            assertEquals(1_000_054L, parser.getSqlFileHeader(resource).getVer());
            assertDoesNotThrow(() -> parser.loadAndValidateSqlHeaders("fa-base", dialect));
        }
    }
}
