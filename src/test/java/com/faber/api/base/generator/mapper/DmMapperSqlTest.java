package com.faber.api.base.generator.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.faber.api.base.telemetry.entity.ClientErrorEvent;
import com.faber.api.base.telemetry.entity.StatEvent;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DmMapperSqlTest {

    @Test
    void shouldUseNativeCatalogForGeneratorQueries() throws Exception {
        var config = new MybatisConfiguration();
        config.setDatabaseId("dm8");
        String path = "mapper/base/generator/GeneratorMapper.xml";
        try (var input = new ClassPathResource(path).getInputStream()) {
            new XMLMapperBuilder(input, config, path, config.getSqlFragments()).parse();
        }
        for (String id : new String[]{"queryTable", "getTableByName", "queryColumns"}) {
            String sql = config.getMappedStatement("com.faber.api.base.generator.mapper.GeneratorMapper." + id)
                    .getBoundSql(Map.of("tableName", "base_user", "query", Map.of("tableName", "user", "tableComment", "用户")))
                    .getSql();
            assertTrue(sql.contains("CURRENT_SCHEMA"));
            assertFalse(sql.contains("information_schema"));
        }
    }

    @Test
    void shouldQuoteTelemetryContextInGeneratedCrudSql() {
        var config = new MybatisConfiguration();
        GlobalConfigUtils.setGlobalConfig(config, new GlobalConfig()
                .setDbConfig(new GlobalConfig.DbConfig().setColumnFormat("\"%s\"")));
        var assistant = new MapperBuilderAssistant(config, "dm8");
        assistant.setCurrentNamespace("dm8");
        for (Class<?> entity : new Class<?>[]{ClientErrorEvent.class, StatEvent.class}) {
            var info = TableInfoHelper.initTableInfo(assistant, entity);
            assertTrue(info.getFieldList().stream()
                    .anyMatch(field -> field.getProperty().equals("context") && field.getColumn().equals("\"context\"")));
        }
    }
}
