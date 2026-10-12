package com.faber.config.auth;

import lombok.Data;

import java.io.Serializable;

/** 非凭证性的客户端标识，用于将后台登录会话对应到设备实例。 */
@Data
public class OnlineUserClientInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String clientType;
    private String clientInstanceId;
}
