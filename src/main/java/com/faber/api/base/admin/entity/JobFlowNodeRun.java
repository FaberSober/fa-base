package com.faber.api.base.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

/** 执行日志表，无逻辑删除与配置审计字段。 */
@Data
@TableName("base_job_flow_node_run")
public class JobFlowNodeRun {
    @TableId(type = IdType.AUTO) private Integer id;
    private Integer flowLogId;
    private String nodeId;
    private String clazzPath;
    private String nodeName;
    private Integer attempt;
    private Integer jobLogId;
    private Integer status;
    private Date beginTime;
    private Date endTime;
    private String reason;
    private Date crtTime;
    private Date updTime;
}
