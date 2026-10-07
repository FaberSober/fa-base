package com.faber.api.base.push.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.bean.BaseUpdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** A Web Push subscription bound to the authenticated user and browser endpoint. */
@Data
@EqualsAndHashCode(callSuper = true)
@FaModalName(name = "BASE-浏览器推送订阅")
@TableName("base_browser_push_subscription")
public class BrowserPushSubscription extends BaseUpdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @SqlEquals
    private String userId;

    private String endpointHash;

    @ToString.Exclude
    private String endpoint;

    @ToString.Exclude
    private String p256dh;

    @ToString.Exclude
    private String auth;
}
