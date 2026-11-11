package com.faber.api.base.admin.vo.ret;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class JobTimelineEntry {
    private Integer id;
    private Integer jobId;
    private String jobName;
    private Integer jobType;
    private String cron;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private Long duration;
    private Integer status;
}
