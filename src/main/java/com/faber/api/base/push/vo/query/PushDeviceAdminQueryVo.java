package com.faber.api.base.push.vo.query;

import lombok.Data;

@Data
public class PushDeviceAdminQueryVo {
    private String userId;
    private String platform;
    private String appId;
    private String environment;
    private Boolean enabled;
}
