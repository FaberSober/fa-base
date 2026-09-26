package com.faber.api.base.push.unipush;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** UniPush 2.0 settings for calling the URLized UniCloud push function. */
@Getter
@Setter
@ConfigurationProperties(prefix = "fa.push.unipush")
public class UniPushProperties {

    /** UniApp runtime AppID (for example, __UNI__...), used to match registered devices. */
    private String clientAppId = "";
    /** Public HTTP URL of the authenticated UniCloud push function. */
    private String cloudFunctionUrl = "";
    /** Shared HMAC secret; configure the same value in the UniCloud function environment. */
    private String cloudFunctionSecret = "";
    private List<String> allowedEnvironments = List.of("development", "test", "staging");
    private int connectTimeoutSeconds = 5;
    private int requestTimeoutSeconds = 15;

    public boolean isConfigured() {
        return hasText(clientAppId) && hasText(cloudFunctionUrl) && hasText(cloudFunctionSecret);
    }

    public boolean supports(PushDeviceIdentity device) {
        if (device == null || !isConfigured() || !hasText(clientAppId)
                || !clientAppId.equals(device.appId())) {
            return false;
        }
        String environment = device.environment() == null ? "" : device.environment().trim();
        return allowedEnvironments != null && allowedEnvironments.stream()
                .anyMatch(allowed -> allowed != null && allowed.equalsIgnoreCase(environment));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record PushDeviceIdentity(String appId, String environment) {
    }
}
