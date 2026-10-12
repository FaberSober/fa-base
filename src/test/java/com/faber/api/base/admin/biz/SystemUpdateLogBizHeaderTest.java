package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.vo.dto.FaSqlHeader;
import com.faber.api.base.FaBaseDbInit;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemUpdateLogBizHeaderTest {

    private final SystemUpdateLogBiz systemUpdateLogBiz = new SystemUpdateLogBiz();

    @Test
    void shouldParseValidHeader() throws Exception {
        Resource resource = sqlResource(
                "1.0.27_test.sql",
                """
                        -- ------------------------- info -------------------------
                        -- @@ver: 1_000_027
                        -- @@info: 测试升级
                        -- ------------------------- info -------------------------

                        SELECT 1;
                        """
        );

        FaSqlHeader header = systemUpdateLogBiz.getSqlFileHeader(resource);

        assertEquals(1_000_027L, header.getVer());
        assertEquals("V1.0.27", header.getVerNo());
        assertEquals("测试升级", header.getInfo());
        assertEquals("1.0.27_test.sql", header.getFileName());
        assertEquals(64, header.getChecksum().length());
    }

    @Test
    void shouldRejectMissingHeaderSplitter() {
        Resource resource = sqlResource("1.0.27_test.sql", "-- @@ver: 1_000_027\nSELECT 1;");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> systemUpdateLogBiz.getSqlFileHeader(resource)
        );

        assertTrue(exception.getMessage().contains("分隔线必须且只能出现两次"));
    }

    @Test
    void shouldRejectFilenameVersionMismatch() {
        Resource resource = sqlResource(
                "1.0.28_test.sql",
                """
                        -- ------------------------- info -------------------------
                        -- @@ver: 1_000_027
                        -- @@info: 测试升级
                        -- ------------------------- info -------------------------

                        SELECT 1;
                        """
        );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> systemUpdateLogBiz.getSqlFileHeader(resource)
        );

        assertTrue(exception.getMessage().contains("文件名版本与@@ver不一致"));
    }

    @Test
    void shouldCleanDuplicateLogsBeforeCreatingUniqueIndex() throws Exception {
        Resource resource = new ClassPathResource("sql/fa-base/mysql/1.0.27_base_system_update_log.sql");
        String sql;
        try (var inputStream = resource.getInputStream()) {
            sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        int duplicateCleanupIndex = sql.indexOf("DELETE duplicate_log");
        int uniqueIndexCreationIndex = sql.indexOf("ADD UNIQUE KEY");

        assertTrue(duplicateCleanupIndex >= 0, "迁移脚本必须先清理历史重复日志");
        assertTrue(uniqueIndexCreationIndex > duplicateCleanupIndex, "唯一索引必须在历史数据去重后创建");
    }

    @Test
    void shouldAddPortalAdminEnabledColumnIdempotentlyForPostgreSql() throws Exception {
        Resource resource = new ClassPathResource("sql/fa-base/postgre/1.0.26_base_portal_mvp_ddl.sql");
        String sql;
        try (var inputStream = resource.getInputStream()) {
            sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS \"admin_enabled\""));
    }

    @Test
    void shouldUseBooleanForPostgreSqlLogMobileColumns() throws Exception {
        Resource resource = new ClassPathResource("sql/fa-base/postgre/1.0.0_base_ddl.sql");
        String sql;
        try (var inputStream = resource.getInputStream()) {
            sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertEquals(2, sql.split("\\\"mobile\\\" boolean DEFAULT NULL", -1).length - 1);
        assertTrue(sql.contains("\"is_read\" boolean NOT NULL DEFAULT false"));
    }

    @Test
    void shouldResolveSupportedDatabaseTypes() {
        assertEquals("mysql", SystemUpdateLogBiz.resolveDatabaseType("MySQL"));
        assertEquals("postgre", SystemUpdateLogBiz.resolveDatabaseType("PostgreSQL"));
        assertEquals("oracle", SystemUpdateLogBiz.resolveDatabaseType("Oracle"));
        assertEquals("dm8", SystemUpdateLogBiz.resolveDatabaseType("DM DBMS"));
        assertEquals("dm8", SystemUpdateLogBiz.resolveDatabaseType("DM8"));
    }

    @Test
    void shouldUseDmColumnDefinitionOrder() throws Exception {
        Resource resource = new ClassPathResource("sql/fa-base/dm8/1.0.10_base_ddl.sql");
        String sql;
        try (var inputStream = resource.getInputStream()) {
            sql = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }

        assertTrue(sql.contains("IDENTITY(1,1) NOT NULL"));
        assertFalse(sql.contains("NOT NULL DEFAULT"));
        assertFalse(sql.contains("END; NOT NULL"));
    }

    @Test
    void shouldRejectDmDatabaseWithoutHistoricalBaselineBeforeExecutingUpgrades() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("SELECT 1 FROM base_system_update_log WHERE 1 = 0")).thenReturn(statement);
        when(statement.executeQuery()).thenThrow(new SQLException("无效的表或视图名", "22000", -2106));
        systemUpdateLogBiz.dataSource = dataSource;

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod(systemUpdateLogBiz, "initOneBuzz", new FaBaseDbInit(), "dm8", false, false));
        assertTrue(exception.getMessage().contains("至少完成 V1.0.9"));
    }

    @Test
    void shouldRejectUnsupportedDatabaseType() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> SystemUpdateLogBiz.resolveDatabaseType("SQLite")
        );

        assertTrue(exception.getMessage().contains("不支持的数据库类型"));
    }

    @Test
    void shouldLoadSqlFromDatabaseTypeDirectory() throws Exception {
        var mysql = systemUpdateLogBiz.loadAndValidateSqlHeaders("fa-base", "mysql");
        var postgre = systemUpdateLogBiz.loadAndValidateSqlHeaders("fa-base", "postgre");
        var dm = systemUpdateLogBiz.loadAndValidateSqlHeaders("fa-base", "dm8");
        assertFalse(mysql.isEmpty());
        assertEquals(mysql.stream().map(FaSqlHeader::getVer).toList(), postgre.stream().map(FaSqlHeader::getVer).toList());
        assertEquals(mysql.stream().filter(h -> h.getVer() >= 1_000_010L).map(FaSqlHeader::getVer).toList(),
                dm.stream().map(FaSqlHeader::getVer).toList());
        assertFalse(systemUpdateLogBiz.loadAndValidateSqlHeaders("fa-base", "oracle").isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenDatabaseTypeDirectoryIsMissing() throws Exception {
        assertTrue(systemUpdateLogBiz.loadAndValidateSqlHeaders("fa-base", "unsupported").isEmpty());
    }

    private Resource sqlResource(String fileName, String sql) {
        return new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
    }

}
