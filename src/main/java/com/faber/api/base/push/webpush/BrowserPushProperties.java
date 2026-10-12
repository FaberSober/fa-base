package com.faber.api.base.push.webpush;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "fa.push.webpush")
public class BrowserPushProperties {

    /** Public VAPID key exposed to authenticated browser clients. */
    private String vapidPublicKey = "";

    /** Private VAPID key used only by the server to send push messages. */
    private String vapidPrivateKey = "";

    /** Contact URI included in VAPID authorization. */
    private String vapidSubject = "";
}
