package com.faber.config.mybatis.interceptor;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.faber.core.constant.CommonConstants;
import com.faber.core.config.mybatis.interceptor.FaTenantInterceptor;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.BuzzException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
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
}
