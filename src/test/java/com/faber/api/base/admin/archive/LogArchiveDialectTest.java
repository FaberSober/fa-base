package com.faber.api.base.admin.archive;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogArchiveDialectTest {

    private final LogArchiveDialectResolver resolver = new LogArchiveDialectResolver(List.of(
            new MySqlLogArchiveDialect(),
            new PostgreSqlLogArchiveDialect(),
            new OracleLogArchiveDialect()
    ));

    @Test
    void resolvesAllSupportedDatabaseProducts() {
        assertInstanceOf(MySqlLogArchiveDialect.class, resolver.resolve("MySQL"));
        assertInstanceOf(PostgreSqlLogArchiveDialect.class, resolver.resolve("PostgreSQL"));
        assertInstanceOf(OracleLogArchiveDialect.class, resolver.resolve("Oracle"));
    }

    @Test
    void rejectsUnsafeArchiveTableNamesBeforeExecutingSql() {
        assertThrows(IllegalArgumentException.class,
                () -> new MySqlLogArchiveDialect().createArchiveTable(null, "base_log_api", "base_log_api;drop"));
    }

    @Test
    void rejectsUnsupportedDatabaseProduct() {
        assertThrows(IllegalStateException.class, () -> resolver.resolve("H2"));
    }

    @Test
    void deletesOnlyRowsConfirmedInArchiveAcrossAllDialects() throws SQLException {
        Timestamp start = Timestamp.from(Instant.parse("2026-08-01T00:00:00Z"));
        Timestamp end = Timestamp.from(Instant.parse("2026-09-01T00:00:00Z"));

        assertCopiedRowQuery(new MySqlLogArchiveDialect(), start, end, "EXISTS (SELECT 1 FROM base_log_api_2026_08 a", "LIMIT ?");
        assertCopiedRowQuery(new PostgreSqlLogArchiveDialect(), start, end, "EXISTS (SELECT 1 FROM base_log_api_2026_08 a", "LIMIT ?");
        assertCopiedRowQuery(new OracleLogArchiveDialect(), start, end, "EXISTS (SELECT 1 FROM base_log_api_2026_08 a", "ROWNUM <= ?");
    }

    @Test
    void cleanupOperationsAreIdempotentAcrossAllDialects() throws SQLException {
        assertDropSql(new MySqlLogArchiveDialect(), "DROP TABLE IF EXISTS base_log_api_2026_08");
        assertDropSql(new PostgreSqlLogArchiveDialect(), "DROP TABLE IF EXISTS base_log_api_2026_08");
        assertDropSql(new OracleLogArchiveDialect(), "DROP TABLE base_log_api_2026_08");
    }

    private void assertCopiedRowQuery(
            LogArchiveDialect dialect,
            Timestamp start,
            Timestamp end,
            String expectedExists,
            String expectedLimit
    ) throws SQLException {
        AtomicReference<String> sql = new AtomicReference<>();
        dialect.findCopiedSourceIds(connection(sql), "base_log_api", "base_log_api_2026_08", start, end, 1000);
        assertTrue(sql.get().contains(expectedExists));
        assertTrue(sql.get().contains(expectedLimit));
    }

    private void assertDropSql(LogArchiveDialect dialect, String expectedSql) throws SQLException {
        AtomicReference<String> sql = new AtomicReference<>();
        dialect.dropTable(connection(sql), "base_log_api_2026_08");
        assertTrue(sql.get().equals(expectedSql));
    }

    private Connection connection(AtomicReference<String> sql) {
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{Connection.class}, (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                sql.set((String) args[0]);
                return statement();
            }
            return defaultValue(method.getReturnType());
        });
    }

    private PreparedStatement statement() {
        return (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{PreparedStatement.class}, (proxy, method, args) -> {
            if ("executeQuery".equals(method.getName())) {
                return resultSet();
            }
            return defaultValue(method.getReturnType());
        });
    }

    private ResultSet resultSet() {
        return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{ResultSet.class}, (proxy, method, args) -> {
            if ("next".equals(method.getName())) {
                return false;
            }
            return defaultValue(method.getReturnType());
        });
    }

    private Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0F;
        }
        if (type == double.class) {
            return 0D;
        }
        if (type == char.class) {
            return '\0';
        }
        return null;
    }
}
