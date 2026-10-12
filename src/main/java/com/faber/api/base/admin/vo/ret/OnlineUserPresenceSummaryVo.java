package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/** 按用户聚合的当前在线调试数量。 */
@Data
public class OnlineUserPresenceSummaryVo {
    private String userId;
    private String username;
    private String name;
    private long webCount;
    private long appCount;
    private long desktopCount;
    private long deviceCount;
    private long lastSeenAt;
}
