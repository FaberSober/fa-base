package com.faber.api.base.admin.archive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** 日志归档方言公共实现。 */
abstract class AbstractLogArchiveDialect implements LogArchiveDialect {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

    @Override
    public long countRows(Connection connection, String tableName, Timestamp startTime, Timestamp endTime) throws SQLException {
        String safeTable = requireTableName(tableName);
        String sql = "SELECT COUNT(*) FROM " + safeTable + " WHERE crt_time >= ? AND crt_time < ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, startTime);
            statement.setTimestamp(2, endTime);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }

    @Override
    public int copyMissingRows(
            Connection connection,
            String sourceTable,
            String archiveTable,
            Timestamp startTime,
            Timestamp endTime
    ) throws SQLException {
        String safeSourceTable = requireTableName(sourceTable);
        String safeArchiveTable = requireTableName(archiveTable);
        String sql = "INSERT INTO " + safeArchiveTable + " SELECT s.* FROM " + safeSourceTable + " s "
                + "WHERE s.crt_time >= ? AND s.crt_time < ? "
                + "AND NOT EXISTS (SELECT 1 FROM " + safeArchiveTable + " a WHERE a.id = s.id)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, startTime);
            statement.setTimestamp(2, endTime);
            return statement.executeUpdate();
        }
    }

    @Override
    public int deleteByIds(Connection connection, String tableName, List<Long> ids) throws SQLException {
        if (ids.isEmpty()) {
            return 0;
        }
        String safeTable = requireTableName(tableName);
        String placeholders = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        String sql = "DELETE FROM " + safeTable + " WHERE id IN (" + placeholders + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < ids.size(); i++) {
                statement.setLong(i + 1, ids.get(i));
            }
            return statement.executeUpdate();
        }
    }

    @Override
    public void dropTable(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DROP TABLE " + requireTableName(tableName))) {
            statement.execute();
        }
    }

    protected List<Long> findIdsWithLimit(
            Connection connection,
            String sql,
            Timestamp startTime,
            Timestamp endTime,
            int batchSize
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, startTime);
            statement.setTimestamp(2, endTime);
            statement.setInt(3, batchSize);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Long> ids = new ArrayList<>();
                while (resultSet.next()) {
                    ids.add(resultSet.getLong(1));
                }
                return ids;
            }
        }
    }

    protected String requireTableName(String tableName) {
        if (tableName == null || !TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法日志归档表名：" + tableName);
        }
        return tableName;
    }
}
