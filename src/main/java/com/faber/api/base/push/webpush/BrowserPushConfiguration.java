package com.faber.api.base.push.webpush;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BrowserPushProperties.class)
public class BrowserPushConfiguration {
}
