package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/** 按账号聚合的有效后台登录会话。 */
@Data
public class OnlineUserSessionUserVo {
    private String userId;
    private String username;
    private String name;
    private long sessionCount;
    private long activeSessionCount;
    private long lastAccessTime;
    private boolean currentUser;
}
