package com.faber.api.base.msg.vo;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class MsgStatisticVO {

    /**
     * 未读消息数量
     */
    private Long unreadCount;

    /** 系统消息未读数量 */
    private Long systemUnreadCount;

    /** 流程消息未读数量 */
    private Long flowUnreadCount;

}
