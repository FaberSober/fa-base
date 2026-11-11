package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/** 时间轴当日统计；耗时为毫秒，仅统计有耗时记录的执行。 */
@Data
public class JobTimelineStats {
    private long total;
    private long success;
    private long failed;
    /** 已结束（非执行中、非停止中）的执行数，作为成功率分母。 */
    private long finished;
    private Long avgDurationMs;
    private Long maxDurationMs;
}
