package com.faber.api.base.admin.archive;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
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
    public List<Long> findIds(Connection connection, String tableName, Timestamp startTime, Timestamp endTime, int batchSize) throws SQLException {
        String sql = "SELECT id FROM " + requireTableName(tableName)
                + " WHERE crt_time >= ? AND crt_time < ? ORDER BY id LIMIT ?";
        return findIdsWithLimit(connection, sql, startTime, endTime, batchSize);
    }
}
