package com.faber.api.portal.push.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortalPushDeviceBindingReqVo {

    @NotBlank
    @Pattern(regexp = "(?i)unipush")
    @Size(max = 32)
    private String provider;

    @NotBlank
    @Size(max = 255)
    private String clientId;

    @NotBlank
    @Size(max = 128)
    private String appId;

    @NotBlank
    @Pattern(regexp = "(?i)android|ios")
    @Size(max = 32)
    private String platform;

    @NotBlank
    @Pattern(regexp = "(?i)development|test|staging|production")
    @Size(max = 16)
    private String environment;

    @Size(max = 255)
    private String previousClientId;
}
