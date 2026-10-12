package com.faber.api.base.telemetry.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.annotation.SqlSearch;
import com.faber.core.bean.BaseDelEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/** 接入 Telemetry 的客户端应用。 */
@Data
@ToString
@EqualsAndHashCode(callSuper = true)
@FaModalName(name = "Telemetry 应用")
@TableName("base_telemetry_app")
public class TelemetryApp extends BaseDelEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @SqlSearch
    private String appKey;

    @SqlSearch
    private String appCode;

    @SqlSearch
    private String appName;

    @SqlEquals
    private TelemetryClientTypeEnum clientType;

    @SqlEquals
    private Boolean enabled;

    private String remark;
}
