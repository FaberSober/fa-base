package com.faber.api.portal.contact.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PortalContactInquiryReqVo {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 200)
    private String company;

    @NotBlank
    @Size(max = 32)
    private String tel;

    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 200)
    private String subject;

    @NotBlank
    @Size(max = 4000)
    private String message;
}
