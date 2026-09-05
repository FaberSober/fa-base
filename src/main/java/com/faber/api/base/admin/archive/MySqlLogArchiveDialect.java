package com.faber.api.base.admin.archive;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/** MySQL 日志归档方言。 */
@Component
public class MySqlLogArchiveDialect extends AbstractLogArchiveDialect {

    @Override
    public boolean supports(String databaseProductName) {
        return databaseProductName != null && databaseProductName.toLowerCase(java.util.Locale.ROOT).contains("mysql");
    }

    @Override
    public void createArchiveTable(Connection connection, String sourceTable, String archiveTable) throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS " + requireTableName(archiveTable)
                + " LIKE " + requireTableName(sourceTable);
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        }
    }

    @Override
    public boolean tableExists(Connection connection, String tableName) throws SQLException {
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, requireTableName(tableName));
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    @Override
    public List<Long> findCopiedSourceIds(
            Connection connection,
            String sourceTable,
            String archiveTable,
            Timestamp startTime,
            Timestamp endTime,
            int batchSize
    ) throws SQLException {
        String sql = "SELECT s.id FROM " + requireTableName(sourceTable) + " s "
                + "WHERE s.crt_time >= ? AND s.crt_time < ? "
                + "AND EXISTS (SELECT 1 FROM " + requireTableName(archiveTable) + " a WHERE a.id = s.id) "
                + "ORDER BY s.id LIMIT ?";
        return findIdsWithLimit(connection, sql, startTime, endTime, batchSize);
    }

    @Override
    public void dropTable(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DROP TABLE IF EXISTS " + requireTableName(tableName))) {
            statement.execute();
        }
    }
}
