package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.vo.dto.FaSqlHeader;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    }

    @Test
    void shouldRejectUnsupportedDatabaseType() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> SystemUpdateLogBiz.resolveDatabaseType("Oracle")
        );

        assertTrue(exception.getMessage().contains("不支持的数据库类型"));
    }

    @Test
    void shouldLoadSqlFromDatabaseTypeDirectory() throws Exception {
        assertEquals(36, systemUpdateLogBiz.loadAndValidateSqlHeaders("fa-base", "mysql").size());
        assertEquals(36, systemUpdateLogBiz.loadAndValidateSqlHeaders("fa-base", "postgre").size());
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
