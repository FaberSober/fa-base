package com.faber.api.base.admin.archive;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

/** Oracle 11.2 日志归档方言。 */
@Component
public class OracleLogArchiveDialect extends AbstractLogArchiveDialect {

    @Override
    public boolean supports(String databaseProductName) {
        return databaseProductName != null && databaseProductName.toLowerCase(java.util.Locale.ROOT).contains("oracle");
    }

    @Override
    public void ensureArchiveMetadataTable(Connection connection) throws SQLException {
        executeIgnoreAlreadyExists(connection, "CREATE TABLE base_log_archive ("
                + "id NUMBER(19) NOT NULL, log_type VARCHAR2(32) NOT NULL, source_table VARCHAR2(128) NOT NULL, "
                + "archive_table VARCHAR2(128) NOT NULL, archive_month VARCHAR2(7) NOT NULL, "
                + "data_start_time TIMESTAMP NULL, data_end_time TIMESTAMP NULL, row_count NUMBER(19) DEFAULT 0 NOT NULL, "
                + "status VARCHAR2(16) NOT NULL, archive_time TIMESTAMP NULL, error_message CLOB NULL, "
                + "crt_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, crt_user VARCHAR2(32) NOT NULL, "
                + "crt_name VARCHAR2(255) NOT NULL, crt_host VARCHAR2(255) NULL, upd_time TIMESTAMP NULL, "
                + "upd_user VARCHAR2(32) NULL, upd_name VARCHAR2(255) NULL, upd_host VARCHAR2(255) NULL, "
                + "CONSTRAINT pk_base_log_archive PRIMARY KEY (id), "
                + "CONSTRAINT uk_base_log_archive_type_month UNIQUE (log_type, archive_month))");
        executeIgnoreAlreadyExists(connection, "CREATE SEQUENCE base_log_archive_seq START WITH 1 INCREMENT BY 1");
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE OR REPLACE TRIGGER base_log_archive_bi BEFORE INSERT ON base_log_archive FOR EACH ROW "
                        + "WHEN (new.id IS NULL) BEGIN SELECT base_log_archive_seq.NEXTVAL INTO :new.id FROM dual; END")) {
            statement.execute();
        }
    }

    @Override
    public void createArchiveTable(Connection connection, String sourceTable, String archiveTable) throws SQLException {
        createIndexIfMissing(connection, "idx_base_log_api_crt", sourceTable, "crt_time");
        createIndexIfMissing(connection, "idx_base_log_api_user_crt", sourceTable, "crt_user, crt_time");
        String sql = "CREATE TABLE " + requireTableName(archiveTable)
                + " AS SELECT * FROM " + requireTableName(sourceTable) + " WHERE 1 = 0";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            // ORA-00955：对象已存在；重试归档时复用原归档表。
            if (e.getErrorCode() != 955) {
                throw e;
            }
        }
        createIndexIfMissing(connection, archiveTable + "_crt", archiveTable, "crt_time");
        createIndexIfMissing(connection, archiveTable + "_user_crt", archiveTable, "crt_user, crt_time");
    }

    @Override
    public boolean tableExists(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_tables WHERE table_name = ?")) {
            statement.setString(1, requireTableName(tableName).toUpperCase(java.util.Locale.ROOT));
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
        String sql = "SELECT id FROM (SELECT s.id FROM " + requireTableName(sourceTable) + " s "
                + "WHERE s.crt_time >= ? AND s.crt_time < ? "
                + "AND EXISTS (SELECT 1 FROM " + requireTableName(archiveTable) + " a WHERE a.id = s.id) "
                + "ORDER BY s.id) WHERE ROWNUM <= ?";
        return findIdsWithLimit(connection, sql, startTime, endTime, batchSize);
    }

    @Override
    public void dropTable(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("DROP TABLE " + requireTableName(tableName))) {
            statement.execute();
        } catch (SQLException e) {
            // ORA-00942：前一次清理已成功删除表，但元数据状态尚未更新。
            if (e.getErrorCode() != 942) {
                throw e;
            }
        }
    }

    private void createIndexIfMissing(Connection connection, String indexName, String tableName, String columns) throws SQLException {
        String sql = "CREATE INDEX " + requireTableName(indexName) + " ON " + requireTableName(tableName) + " (" + columns + ")";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            // ORA-00955：索引已经存在。
            if (e.getErrorCode() != 955) {
                throw e;
            }
        }
    }

    private void executeIgnoreAlreadyExists(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException e) {
            if (e.getErrorCode() != 955) {
                throw e;
            }
        }
    }
}
