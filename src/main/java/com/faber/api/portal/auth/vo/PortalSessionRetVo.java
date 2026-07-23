package com.faber.api.portal.auth.vo;

import cn.dev33.satoken.stp.SaTokenInfo;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalSessionRetVo {

    private String token;
    private PortalUserRetVo user;

    public static PortalSessionRetVo of(SaTokenInfo tokenInfo, PortalUserRetVo user) {
        return PortalSessionRetVo.builder()
                .token(tokenInfo.getTokenValue())
                .user(user)
                .build();
    }
}
