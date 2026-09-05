package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/** 在线后台会话的公开信息，不包含登录凭证。时间为 Unix 毫秒。 */
@Data
public class OnlineUserVo {
    private String id;
    private String userId;
    private String username;
    private String name;
    private String source;
    private Long loginTime;
    private long lastAccessTime;
    private String ip;
    private String browser;
    private String os;
    private Long expiresAt;
    private boolean active;
    private boolean current;
    private boolean currentUser;
}
