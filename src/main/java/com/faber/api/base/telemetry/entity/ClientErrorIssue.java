package com.faber.api.base.telemetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.api.base.telemetry.enums.TelemetryIssueStatusEnum;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.annotation.SqlSearch;
import lombok.Data;

import java.util.Date;

/** 同类客户端异常的聚合 Issue。 */
@Data
@TableName("base_client_error_issue")
public class ClientErrorIssue {

    @TableId(type = IdType.AUTO)
    private Long id;

    @SqlEquals
    private Long appId;

    @SqlEquals
    private TelemetryClientTypeEnum clientType;

    private String fingerprint;

    @SqlSearch
    private String title;

    @SqlEquals
    private String errorType;

    @SqlEquals
    private TelemetryIssueStatusEnum status;

    private Date firstSeenTime;
    private Date lastSeenTime;
    private Long eventCount;
    private Long userCount;

    @SqlEquals
    private String latestRelease;

    private Date createTime;
    private Date updateTime;
}
