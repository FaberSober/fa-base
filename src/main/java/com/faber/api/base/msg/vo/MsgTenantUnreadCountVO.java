package com.faber.api.base.msg.vo;

import lombok.Data;

@Data
public class MsgTenantUnreadCountVO {

    private String tenantId;

    private Long unreadCount;
}
