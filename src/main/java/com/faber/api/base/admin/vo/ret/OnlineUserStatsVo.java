package com.faber.api.base.admin.vo.ret;

import lombok.Data;

@Data
public class OnlineUserStatsVo {
    private long sessionCount;
    private long userCount;
    private long activeUserCount;
    private long activeWindowSeconds;
}
