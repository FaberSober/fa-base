package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/** 在线 WebSocket 客户端公开信息，不包含 Token 或 Session。时间为 Unix 毫秒。 */
@Data
public class RemoteClientVo {
    private String id;
    private String clientType;
    private String runtime;
    private String appCode;
    private String appName;
    private String release;
    private String environment;
    private String platform;
    private String osName;
    private String osVersion;
    private String deviceModel;
    private String userId;
    private String username;
    private String name;
    private long connectedAt;
    private long lastSeenAt;
}
