package com.faber.api.portal.auth.vo;

import lombok.Data;

import java.util.Date;

/** 登录设备列表的安全展示字段。 */
@Data
public class PortalLoginDeviceRetVo {

    private Integer id;
    private String displayName;
    private String os;
    private String osVersion;
    private Date firstLoginTime;
    private Date lastLoginTime;
    private Boolean current;
    private String trustStatus;
}
