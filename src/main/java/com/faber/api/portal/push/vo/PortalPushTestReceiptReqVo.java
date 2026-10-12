package com.faber.api.portal.push.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortalPushTestReceiptReqVo {

    @NotBlank
    @Pattern(regexp = "(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    private String testId;

    @NotBlank
    @Pattern(regexp = "received|clicked")
    private String event;

    @NotBlank
    @Size(max = 255)
    private String clientId;

    @NotBlank
    @Size(max = 128)
    private String appId;

    @NotBlank
    @Pattern(regexp = "development|test|staging|production")
    private String environment;
}
