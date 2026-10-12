package com.faber.api.base.push.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.bean.BaseDelEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * BASE-推送设备
 */
@Data
@EqualsAndHashCode(callSuper = true)
@FaModalName(name = "BASE-推送设备")
@TableName("base_push_device")
public class PushDevice extends BaseDelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @SqlEquals
    private String userId;

    @SqlEquals
    private String provider;

    private String clientId;

    private String appId;

    @SqlEquals
    private String platform;

    @SqlEquals
    private String environment;

    @SqlEquals
    private Boolean enabled;

    private Date lastSeenTime;

    private Date invalidTime;
}
