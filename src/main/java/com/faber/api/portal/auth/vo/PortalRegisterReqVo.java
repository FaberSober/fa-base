package com.faber.api.portal.auth.vo;

import com.faber.core.config.validator.validator.TelNoValidator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortalRegisterReqVo {

    @NotBlank
    @Size(max = 64)
    private String username;

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @TelNoValidator
    private String tel;

    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    @NotBlank
    private String passwordConfirm;
}
