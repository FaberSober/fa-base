package com.faber.api.base.push.unipush;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(UniPushProperties.class)
public class UniPushConfiguration {
}
