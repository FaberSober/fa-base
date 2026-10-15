package com.faber.api.base.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "fa.security.crypto")
public class RequestCryptoProperties {

    private String kid = "v1";
    private String privateKeyFile;
    private Replay replay = new Replay();

    @Getter
    @Setter
    public static class Replay {
        private boolean enabled = false;
    }
}
