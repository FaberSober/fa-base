package com.faber.api.base.admin.biz;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.statement.SQLUpdateStatement;
import com.faber.api.base.admin.vo.dto.FaSqlHeader;
import com.faber.core.utils.SqlUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobExecutionSqlMigrationTest {

    private static final String FILE_NAME = "1.0.53_base_job_execution_stats.sql";
    private static final List<String> DIALECTS = List.of("mysql", "postgre");

    @Test
    void actualMigrationHeadersPassTheProductionParser() throws Exception {
        SystemUpdateLogBiz parser = new SystemUpdateLogBiz();
        for (String dialect : DIALECTS) {
            FaSqlHeader header = parser.getSqlFileHeader(resource(dialect));
            assertEquals(1_000_053L, header.getVer());
            assertEquals("V1.0.53", header.getVerNo());
            assertEquals(FILE_NAME, header.getFileName());
            assertFalse(header.getInfo().isBlank());
            assertEquals(64, header.getChecksum().length());
        }
    }

    @Test
    void moduleMigrationVersionsRemainUniqueForEachTargetDatabase() throws Exception {
        SystemUpdateLogBiz parser = new SystemUpdateLogBiz();
        for (String dialect : DIALECTS) {
            List<FaSqlHeader> headers = parser.loadAndValidateSqlHeaders("fa-base", dialect);
            assertEquals(1, headers.stream().filter(header -> header.getVer() == 1_000_053L).count());
            assertEquals(headers.size(), headers.stream().map(FaSqlHeader::getVer).distinct().count());
        }
    }

    @Test
    void bothNativeDialectsPassParsingAndTheProductionUpgradeStatementSplitter() throws Exception {
        for (String dialect : DIALECTS) {
            String sql;
            try (var input = resource(dialect).getInputStream()) {
                sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            DbType dbType = dialect.equals("mysql") ? DbType.mysql : DbType.postgresql;
            var parsed = SQLUtils.parseStatements(sql, dbType);
            assertEquals(1, parsed.stream().filter(SQLUpdateStatement.class::isInstance).count());

            Connection connection = mock(Connection.class);
            Statement statement = mock(Statement.class);
            when(connection.createStatement()).thenReturn(statement);

            SqlUtils.executeSql(connection, sql);

            ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
            verify(statement, times(parsed.size())).execute(statements.capture());
            for (String oneStatement : statements.getAllValues()) {
                assertEquals(1, SQLUtils.parseStatements(oneStatement, dbType).size(),
                        "升级执行器应保持每条DDL及回填语句完整");
            }
            verify(connection).setAutoCommit(true);
            verify(statement).close();
        }
    }

    private ClassPathResource resource(String dialect) {
        return new ClassPathResource("sql/fa-base/" + dialect + "/" + FILE_NAME);
    }
}
