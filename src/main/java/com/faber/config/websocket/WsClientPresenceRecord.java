package com.faber.config.websocket;

import lombok.Data;

import java.io.Serializable;

/** Redis 中的短期 WebSocket 在线记录，不包含 Token 或 Session 对象。 */
@Data
public class WsClientPresenceRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private String sessionId;
    private String userId;
    private String clientInstanceId;
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
    private long connectedAt;
    private long lastSeenAt;
}
