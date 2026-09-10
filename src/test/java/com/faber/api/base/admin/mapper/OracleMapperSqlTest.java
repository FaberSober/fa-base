package com.faber.api.base.admin.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleMapperSqlTest {

    @Test
    void shouldUseNumericDeletedValueForOracleMapperSql() throws Exception {
        assertSqlContains("mapper/base/rbac/RbacUserRoleMapper.xml",
                "com.faber.api.base.rbac.mapper.RbacUserRoleMapper.countByUserIdAndLinkUrl", "WHERE deleted = 0");
        assertSqlContains("mapper/base/admin/LogLoginMapper.xml",
                "com.faber.api.base.admin.mapper.LogLoginMapper.countByDay", "WHERE deleted = 0");
        assertSqlContains("mapper/base/admin/ConfigSceneMapper.xml",
                "com.faber.api.base.admin.mapper.ConfigSceneMapper.findMaxSort", "WHERE t.deleted = 0");
    }

    private void assertSqlContains(String resourcePath, String statementId, String expected) throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setDatabaseId("oracle");
        try (InputStream input = new ClassPathResource(resourcePath).getInputStream()) {
            new XMLMapperBuilder(input, configuration, resourcePath, configuration.getSqlFragments()).parse();
        }
        String sql = configuration.getMappedStatement(statementId).getBoundSql(Map.of()).getSql();
        assertTrue(sql.contains(expected));
    }
}
