package com.faber.api.base.push.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class BrowserPushSubscriptionEndpointReqVo {

    @NotBlank
    @Size(max = 2048)
    @ToString.Exclude
    private String endpoint;
}
