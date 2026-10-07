package com.faber.api.base.push.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class BrowserPushTestSendReqVo {

    @NotBlank
    @Size(max = 50)
    @ToString.Exclude
    private String title;

    @NotBlank
    @Size(max = 200)
    @ToString.Exclude
    private String body;
}
