package com.faber.api.portal.auth.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PortalLoginReqVo {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
