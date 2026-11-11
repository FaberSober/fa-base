package com.faber.api.base.admin.mapper;

import com.faber.core.config.mybatis.base.FaBaseMapper;
import com.faber.api.base.admin.entity.JobLog;
import org.apache.ibatis.annotations.Param;

/**
 * BASE-系统定时任务-执行日志
 * 
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-07 16:03:51
 */
public interface JobLogMapper extends FaBaseMapper<JobLog> {
    java.util.List<com.faber.api.base.admin.vo.ret.JobCalendarDay> calendar(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
    /** 窗口内记录数超过 threshold 的任务 ID（需聚合显示的高频任务）。 */
    java.util.List<Integer> timelineHeavyJobs(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end,
            @Param("type") Integer type, @Param("status") Integer status, @Param("keyword") String keyword, @Param("threshold") int threshold);
    /** 窗口内的执行明细，排除 excludeJobIds（这些任务改走 timelineBuckets）。 */
    java.util.List<com.faber.api.base.admin.vo.ret.JobTimelineEntry> timeline(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end,
            @Param("type") Integer type, @Param("status") Integer status, @Param("keyword") String keyword,
            @Param("excludeJobIds") java.util.List<Integer> excludeJobIds, @Param("now") java.time.LocalDateTime now);
    /** 高频任务按 15 分钟分桶的聚合结果；窗口须落在同一天内，bucket 为当天第几个 15 分钟。 */
    java.util.List<com.faber.api.base.admin.vo.ret.JobTimelineBucket> timelineBuckets(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end,
            @Param("type") Integer type, @Param("status") Integer status, @Param("keyword") String keyword,
            @Param("jobIds") java.util.List<Integer> jobIds);
    com.faber.api.base.admin.vo.ret.JobTimelineStats timelineStats(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end,
            @Param("type") Integer type, @Param("status") Integer status, @Param("keyword") String keyword);
    Long recentSuccessAverage(@Param("jobId") Integer jobId);
    int publishDispatch(@Param("record") JobLog record);
    int finishExecution(@Param("record") JobLog record);
}
