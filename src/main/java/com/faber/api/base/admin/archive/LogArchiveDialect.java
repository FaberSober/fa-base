package com.faber.api.base.admin.archive;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/** 不同数据库的日志归档表操作。 */
public interface LogArchiveDialect {

    boolean supports(String databaseProductName);

    default void ensureArchiveMetadataTable(Connection connection) throws SQLException {
        // MySQL / PostgreSQL 由模块升级脚本创建；Oracle 11.2 在运行时补齐兼容表结构。
    }

    void createArchiveTable(Connection connection, String sourceTable, String archiveTable) throws SQLException;

    long countRows(Connection connection, String tableName, Timestamp startTime, Timestamp endTime) throws SQLException;

    int copyMissingRows(Connection connection, String sourceTable, String archiveTable, Timestamp startTime, Timestamp endTime) throws SQLException;

    List<Long> findIds(Connection connection, String tableName, Timestamp startTime, Timestamp endTime, int batchSize) throws SQLException;

    int deleteByIds(Connection connection, String tableName, List<Long> ids) throws SQLException;

    void dropTable(Connection connection, String tableName) throws SQLException;
}
