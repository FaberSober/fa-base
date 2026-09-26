package com.faber.api.base.push.vo.ret;

import lombok.Data;

import java.util.Date;

@Data
public class PushDeviceAdminVo {
    private Long id;
    private String userId;
    private String username;
    private String name;
    private String provider;
    private String clientIdMasked;
    private String platform;
    private String appId;
    private String environment;
    private Boolean enabled;
    private Date lastSeenTime;
    private Date invalidTime;
    private boolean selectable;
}
