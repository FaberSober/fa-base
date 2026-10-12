package com.faber.api.base.admin.archive;

import org.springframework.stereotype.Component;

import java.util.List;

/** 根据 JDBC 数据库产品名称选择日志归档方言。 */
@Component
public class LogArchiveDialectResolver {

    private final List<LogArchiveDialect> dialects;

    public LogArchiveDialectResolver(List<LogArchiveDialect> dialects) {
        this.dialects = dialects;
    }

    public LogArchiveDialect resolve(String databaseProductName) {
        return dialects.stream()
                .filter(dialect -> dialect.supports(databaseProductName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("不支持的日志归档数据库类型：" + databaseProductName));
    }
}
