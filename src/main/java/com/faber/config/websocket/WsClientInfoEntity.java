package com.faber.config.websocket;

import com.faber.api.base.admin.entity.User;
import com.faber.core.config.websocket.ClientInfoEntity;
import lombok.Data;

@Data
public class WsClientInfoEntity extends ClientInfoEntity {

    /** 通用客户端类别，例如 MOBILE、DESKTOP、OTHER。 */
    private String clientType;
    /** 客户端安装实例标识；仅用于识别，不作为可信凭据。 */
    private String clientInstanceId;
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

    /**
     * 用户信息
     */
    private User user;

}
