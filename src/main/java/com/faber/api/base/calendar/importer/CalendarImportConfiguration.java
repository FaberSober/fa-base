package com.faber.api.base.calendar.importer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** 外部日历导入自动配置入口。 */
@Configuration
@EnableConfigurationProperties(CalendarImportProperties.class)
public class CalendarImportConfiguration {
}
