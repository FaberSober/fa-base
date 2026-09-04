package com.faber.api.base.admin.archive;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
