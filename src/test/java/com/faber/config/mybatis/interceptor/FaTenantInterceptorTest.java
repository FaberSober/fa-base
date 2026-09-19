package com.faber.config.mybatis.interceptor;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.faber.core.constant.CommonConstants;
import com.faber.core.config.mybatis.interceptor.FaTenantInterceptor;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.BuzzException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaTenantInterceptorTest {

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
    }

    @Test
    void tenantEntityTableGetsCurrentTenantCondition() {
        BaseContextHandler.setUserId("2");
        TenantContext.setTenantId("tenant-a");
        FaTenantInterceptor interceptor = new FaTenantInterceptor();

        Expression expression = interceptor.buildTableExpression(new Table("base_department"), null, "");

        assertEquals("tenant_id = 'tenant-a'", expression.toString());
        assertFalse(interceptor.getTenantLineHandler().ignoreTable("base_department"));
        assertFalse(interceptor.getTenantLineHandler().ignoreTable("\"base_department\""));
    }

    @Test
    void nonTenantTableIsIgnored() {
        TenantContext.setTenantId("tenant-a");
        FaTenantInterceptor interceptor = new FaTenantInterceptor();

        assertTrue(interceptor.getTenantLineHandler().ignoreTable("base_user"));
        assertNull(interceptor.buildTableExpression(new Table("base_user"), null, ""));
    }

    @Test
    void missingTenantContextIsRejectedForTenantTable() {
        BaseContextHandler.setUserId("2");
        TenantContext.clear();
        FaTenantInterceptor interceptor = new FaTenantInterceptor();

        assertThrows(BuzzException.class,
                () -> interceptor.buildTableExpression(new Table("base_department"), null, ""));
    }

    @Test
    void superAdminWithoutTenantCanUseUnscopedAccess() {
        BaseContextHandler.setUserId(CommonConstants.SUPER_ADMIN_ID);
        TenantContext.clear();
        FaTenantInterceptor interceptor = new FaTenantInterceptor();

        assertTrue(interceptor.getTenantLineHandler().ignoreTable("base_department"));
        assertNull(interceptor.buildTableExpression(new Table("base_department"), null, ""));
    }

    @Test
    void tenantEntityCrudSqlGetsTenantScope() throws Exception {
        BaseContextHandler.setUserId("2");
        TenantContext.setTenantId("tenant-a");
        TestableTenantInterceptor interceptor = new TestableTenantInterceptor();

        assertTrue(interceptor.rewrite("SELECT * FROM base_department WHERE name = '研发'")
                .contains("tenant_id = 'tenant-a'"));
        String insertSql = interceptor.rewrite("INSERT INTO base_department (id, name) VALUES ('1', '研发')");
        assertTrue(insertSql.contains("tenant_id"));
        assertTrue(insertSql.contains("'tenant-a'"));
        assertTrue(interceptor.rewrite("UPDATE base_department SET name = '产品' WHERE id = '1'")
                .contains("tenant_id = 'tenant-a'"));
        assertTrue(interceptor.rewrite("DELETE FROM base_department WHERE id = '1'")
                .contains("tenant_id = 'tenant-a'"));
    }

    private static class TestableTenantInterceptor extends FaTenantInterceptor {

        private String rewrite(String sql) throws Exception {
            Statement statement = CCJSqlParserUtil.parse(sql);
            if (statement instanceof Select) {
                processSelect((Select) statement, 0, sql, null);
            } else if (statement instanceof Insert) {
                processInsert((Insert) statement, 0, sql, null);
            } else if (statement instanceof Update) {
                processUpdate((Update) statement, 0, sql, null);
            } else if (statement instanceof Delete) {
                processDelete((Delete) statement, 0, sql, null);
            }
            return statement.toString();
        }
    }
}
