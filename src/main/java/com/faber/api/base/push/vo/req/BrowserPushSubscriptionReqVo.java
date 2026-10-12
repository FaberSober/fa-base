package com.faber.api.base.push.vo.req;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class BrowserPushSubscriptionReqVo {

    @NotBlank
    @Size(max = 2048)
    @ToString.Exclude
    private String endpoint;

    @NotNull
    @Valid
    @ToString.Exclude
    private Keys keys;

    @Data
    public static class Keys {
        @NotBlank
        @Size(max = 128)
        @ToString.Exclude
        private String p256dh;

        @NotBlank
        @Size(max = 64)
        @ToString.Exclude
        private String auth;
    }
}
