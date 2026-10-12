package com.faber.core.utils;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DmSqlUtilsTest {

    @Test
    void shouldKeepDmIdempotentBlocksIntactAndRejectMysqlDialect() throws Exception {
        var resources = new PathMatchingResourcePatternResolver().getResources("classpath*:sql/fa-base/dm8/*.sql");
        assertTrue(resources.length > 0);
        for (var resource : resources) {
            String sql;
            try (var input = resource.getInputStream()) {
                sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            assertFalse(sql.contains("`"), resource.getFilename());
            assertFalse(sql.contains("information_schema"), resource.getFilename());
            assertFalse(sql.contains("PREPARE stmt"), resource.getFilename());
            for (String statement : SqlUtils.splitSqlStatements(sql)) {
                SqlUtils.validateUpgradeSqlSafety(statement);
                if (statement.contains("EXECUTE IMMEDIATE")) {
                    assertTrue(statement.trim().endsWith("END;"), resource.getFilename());
                    assertTrue(statement.contains("END IF;"), resource.getFilename());
                }
            }
        }
    }
}
