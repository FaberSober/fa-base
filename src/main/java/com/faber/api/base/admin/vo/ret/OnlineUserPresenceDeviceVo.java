package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/** 在线设备的展示信息，不返回实例 ID、连接 ID 或 Token。 */
@Data
public class OnlineUserPresenceDeviceVo {
    private String clientType;
    private String appCode;
    private String appName;
    private String release;
    private String environment;
    private String platform;
    private String osName;
    private String osVersion;
    private String deviceModel;
    private long connectedAt;
    private long lastSeenAt;
}
