package com.faber.api.portal.auth.vo;

import com.faber.api.base.admin.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PortalUserRetVo {

    private String id;
    private String username;
    private String name;
    private String avatar;
    private String tel;
    private String email;
    private Boolean status;
    private Boolean adminEnabled;

    public static PortalUserRetVo from(User user) {
        return PortalUserRetVo.builder()
                .id(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .avatar(user.getImg())
                .tel(user.getTel())
                .email(user.getEmail())
                .status(user.getStatus())
                .adminEnabled(Boolean.TRUE.equals(user.getAdminEnabled()))
                .build();
    }
}
