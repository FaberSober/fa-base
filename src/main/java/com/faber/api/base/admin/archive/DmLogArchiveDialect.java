package com.faber.api.base.admin.archive;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Locale;

/** DM8 原生模式日志归档方言。 */
@Component
public class DmLogArchiveDialect extends AbstractLogArchiveDialect {

    @Override
    public boolean supports(String databaseProductName) {
        return databaseProductName != null && databaseProductName.toLowerCase(Locale.ROOT).startsWith("dm");
    }

    @Override
    public void createArchiveTable(Connection connection, String sourceTable, String archiveTable) throws SQLException {
        String source = requireTableName(sourceTable);
        String archive = requireTableName(archiveTable);
        if (!tableExists(connection, archive)) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE " + archive + " AS SELECT * FROM " + source + " WHERE 1 = 0")) {
                statement.execute();
            }
        }
        // 重试归档时也补齐索引，避免首次执行中途失败留下无索引的归档表。
        for (String columns : List.of("crt_time", "crt_user, crt_time")) {
            String indexName = archive + (columns.startsWith("crt_user") ? "_user_crt" : "_crt");
            try (PreparedStatement statement = connection.prepareStatement(
                    "CREATE INDEX IF NOT EXISTS " + requireTableName(indexName) + " ON " + archive + " (" + columns + ")")) {
                statement.execute();
            }
        }
    }

    @Override
    public boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT 1 FROM ALL_TABLES WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') "
                + "AND LOWER(TABLE_NAME) = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requireTableName(tableName).toLowerCase(Locale.ROOT));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @Override
    public List<Long> findCopiedSourceIds(Connection connection, String sourceTable, String archiveTable,
                                         Timestamp startTime, Timestamp endTime, int batchSize) throws SQLException {
        String sql = "SELECT id FROM (SELECT s.id FROM " + requireTableName(sourceTable) + " s "
                + "WHERE s.crt_time >= ? AND s.crt_time < ? "
                + "AND EXISTS (SELECT 1 FROM " + requireTableName(archiveTable) + " a WHERE a.id = s.id) "
                + "ORDER BY s.id) WHERE ROWNUM <= ?";
        return findIdsWithLimit(connection, sql, startTime, endTime, batchSize);
    }

    @Override
    public void dropTable(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DROP TABLE IF EXISTS " + requireTableName(tableName))) {
            statement.execute();
        }
    }
}
