package com.faber.core.utils;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleSqlUtilsTest {

    @Test
    void shouldKeepFirstOracleBaselineStatementIntact() throws Exception {
        ClassPathResource resource = new ClassPathResource("sql/fa-base/oracle/1.0.0_base_ddl.sql");
        String sql = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        List<String> statements = SqlUtils.splitSqlStatements(sql);

        assertTrue(statements.get(0).contains("CREATE TABLE base_area"));
        assertFalse(statements.get(0).contains("BEGIN EXECUTE IMMEDIATE"));
        assertTrue(sql.contains("TIMESTAMP '2023-04-04 16:10:50'"));
        assertTrue(sql.contains("DATE '2000-01-01'"));
    }
}
