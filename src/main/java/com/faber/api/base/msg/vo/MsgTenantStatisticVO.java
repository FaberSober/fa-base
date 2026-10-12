package com.faber.api.base.msg.vo;

import lombok.Data;

import java.util.Map;

/** 当前用户按租户统计的未读消息数量。 */
@Data
public class MsgTenantStatisticVO {

    private Map<String, Long> tenantUnreadCounts;

    private Long totalUnreadCount;
}
