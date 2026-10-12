package com.faber.api.base.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.api.base.admin.enums.LogArchiveStatusEnum;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.bean.BaseUpdEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/** 日志归档元数据。 */
@Data
@EqualsAndHashCode(callSuper = true)
@FaModalName(name = "日志归档记录")
@TableName("base_log_archive")
public class LogArchive extends BaseUpdEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @SqlEquals
    private String logType;

    private String sourceTable;

    private String archiveTable;

    /** yyyy-MM。 */
    @SqlEquals
    private String archiveMonth;

    private Date dataStartTime;

    private Date dataEndTime;

    private Long rowCount;

    @SqlEquals
    private LogArchiveStatusEnum status;

    private Date archiveTime;

    private String errorMessage;
}
