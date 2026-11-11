package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/** 高频任务在时间轴上的 15 分钟聚合桶。 */
@Data
public class JobTimelineBucket {
    private Integer jobId;
    private String jobName;
    private Integer jobType;
    private String cron;
    /** 当天第几个 15 分钟，0–95。 */
    private Integer bucket;
    private Long total;
    private Long failed;
}
