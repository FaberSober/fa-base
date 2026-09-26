package com.faber.api.base.push.unipush;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Server-side UniPush REST API settings. Secrets must be supplied outside source control. */
@Getter
@Setter
@ConfigurationProperties(prefix = "fa.push.unipush")
public class UniPushProperties {

    private String apiBaseUrl = "https://restapi.getui.com/v2";
    private String appId = "";
    private String appKey = "";
    private String masterSecret = "";
    private List<String> allowedEnvironments = List.of("development", "test", "staging");
    private int connectTimeoutSeconds = 5;
    private int requestTimeoutSeconds = 15;

    public boolean isConfigured() {
        return hasText(appId) && hasText(appKey) && hasText(masterSecret);
    }

    public boolean supports(PushDeviceIdentity device) {
        if (device == null || !isConfigured() || !appId.equals(device.appId())) {
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
