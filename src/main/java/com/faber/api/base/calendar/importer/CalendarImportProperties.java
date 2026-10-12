package com.faber.api.base.calendar.importer;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** 外部年度日历导入配置。 */
@Getter
@Setter
@ConfigurationProperties(prefix = "fa.calendar.import")
public class CalendarImportProperties {

    /** holiday-cn 年度 JSON 地址模板。 */
    private String holidayUrlTemplate = "https://raw.githubusercontent.com/NateScarlet/holiday-cn/master/{year}.json";

    /** AKTools 服务根地址，例如 http://127.0.0.1:8080。 */
    private String akToolsUrl = "";

    private int connectTimeoutSeconds = 5;

    private int requestTimeoutSeconds = 15;

    private int maxResponseBytes = 2_000_000;
}
