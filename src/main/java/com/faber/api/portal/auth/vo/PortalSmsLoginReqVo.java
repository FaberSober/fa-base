package com.faber.api.portal.auth.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PortalSmsLoginReqVo {

    @NotBlank
    @Pattern(regexp = "^1[3-9][0-9]{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$", message = "短信验证码格式不正确")
    private String verificationCode;
}
