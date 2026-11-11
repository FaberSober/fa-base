package com.faber.api.base.admin.biz;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JobVisualMenuSqlTest {
    @Test void menuScriptsHaveValidHeadersAndStatements() throws Exception {
        for (String dialect : List.of("mysql", "postgre")) {
            Path file = Path.of("src/main/resources/sql/fa-base", dialect, "1.0.55_base_job_visual_menu.sql");
            assertEquals(1_000_055L, new SystemUpdateLogBiz().getSqlFileHeader(new FileSystemResource(file)).getVer());
            assertEquals(2, SQLUtils.parseStatements(Files.readString(file), dialect.equals("mysql") ? DbType.mysql : DbType.postgresql).size());
        }
    }
}
