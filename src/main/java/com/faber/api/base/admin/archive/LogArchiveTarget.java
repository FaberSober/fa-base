package com.faber.api.base.admin.archive;

import java.util.Objects;
import java.util.regex.Pattern;

/** 一个可参与归档的日志表声明。 */
public record LogArchiveTarget(String logType, String sourceTable) {

    private static final Pattern LOG_TYPE_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]{0,31}$");
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

    public LogArchiveTarget {
        Objects.requireNonNull(logType, "logType");
        Objects.requireNonNull(sourceTable, "sourceTable");
        if (!LOG_TYPE_PATTERN.matcher(logType).matches() || !TABLE_NAME_PATTERN.matcher(sourceTable).matches()) {
            throw new IllegalArgumentException("非法日志归档目标：" + logType + "/" + sourceTable);
        }
    }

    public String archiveTable( java.time.YearMonth month) {
        return sourceTable + "_" + month.toString().replace('-', '_');
    }
}
