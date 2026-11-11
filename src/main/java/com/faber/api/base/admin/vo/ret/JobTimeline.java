package com.faber.api.base.admin.vo.ret;

import java.time.LocalDateTime;
import java.util.List;

/** entries 为窗口内未聚合任务的明细，buckets 为高频任务的 15 分钟聚合；stats 始终按整天统计。 */
public record JobTimeline(LocalDateTime serverTime, List<JobTimelineEntry> entries, List<JobTimelineBucket> buckets, JobTimelineStats stats) {}
