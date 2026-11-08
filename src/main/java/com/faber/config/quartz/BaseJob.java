package com.faber.config.quartz;

import com.faber.api.base.admin.biz.JobLogBiz;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.config.thread.TaskExecutionTracker;
import com.faber.config.quartz.logging.JobLogFileService;
import com.faber.config.quartz.logging.JobRunLogger;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import jakarta.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 定时任务父类
 * @author xu.pengfei
 * @date 2022/11/28 13:43
 */
@Slf4j
public abstract class BaseJob implements Job {

    @Resource
    private JobLogBiz jobLogBiz;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        log.debug("{}：启动任务=======================", this.getClass().getSimpleName());
        Map<String, Object> previous = BaseContextHandler.getHoldMap();
        BaseContextHandler.setHoldMap(previous == null ? new HashMap<>() : new HashMap<>(previous));
        long startedNanos = System.nanoTime();
        try {
            BaseContextHandler.useAdmin();
            int jobId = context.getJobDetail().getJobDataMap().getIntValue("jobId");
            JobLog jobLog = createLog(jobId);
            JobLogFileService.ExecutionLog executionLog = jobLogBiz.openExecutionLog(jobLog);
            executeManaged(jobLog, executionLog, startedNanos, null);
        } finally {
            if (previous == null) BaseContextHandler.remove();
            else BaseContextHandler.setHoldMap(previous);
        }
        log.debug(printNextFireTime(context));
    }

    /** 调用方已事务性登记本轮节点日志；主方法返回不代表异步工作结束。 */
    public void executeFlowNode(JobLog record, boolean detailLogEnabled, Consumer<JobLog> onFinished) {
        if (record.getId() == null || record.getStatus() != JobLogStatusEnum.DOING) {
            throw new IllegalArgumentException("流程节点必须先登记执行日志");
        }
        Map<String, Object> previous = BaseContextHandler.getHoldMap();
        BaseContextHandler.setHoldMap(previous == null ? new HashMap<>() : new HashMap<>(previous));
        long startedNanos = System.nanoTime();
        try {
            BaseContextHandler.useAdmin();
            var executionLog = jobLogBiz.openExecutionLog(record, detailLogEnabled);
            executeManaged(record, executionLog, startedNanos, onFinished);
        } finally {
            if (previous == null) BaseContextHandler.remove();
            else BaseContextHandler.setHoldMap(previous);
        }
    }

    private void executeManaged(JobLog jobLog, JobLogFileService.ExecutionLog executionLog,
                                long startedNanos, Consumer<JobLog> onFinished) {
        TaskExecutionTracker tracker = new TaskExecutionTracker(
                () -> JobRunLogger.bind(executionLog),
                result -> finishExecution(jobLog, executionLog, startedNanos, result, onFinished));
        Throwable failure = null;
        try (JobRunLogger.Scope ignored = JobRunLogger.bind(executionLog);
             TaskExecutionTracker.Scope tracking = TaskExecutionTracker.bind(tracker)) {
            JobRunLogger.info("任务开始：{}，执行记录：{}", getClass().getSimpleName(), jobLog.getId());
            run();
        } catch (Throwable e) {
            failure = e;
            log.error(e.getMessage(), e);
            if (e instanceof Error error) throw error;
        } finally {
            jobLog.setDispatchEndTime(new Date());
            jobLog.setDispatchDurationMs(elapsedMillis(startedNanos));
            // 先通知主流程退出，避免同步任务把统计写库时间算入业务总耗时。
            tracker.completeMain(failure);
            try {
                jobLogBiz.recordDispatch(jobLog);
            } catch (Exception e) {
                log.error("登记任务触发耗时失败，执行记录：{}", jobLog.getId(), e);
            }
        }
    }

    private void finishExecution(JobLog record, JobLogFileService.ExecutionLog executionLog,
                                 long startedNanos, TaskExecutionTracker.Result result, Consumer<JobLog> onFinished) {
        Throwable failure = result.failure();
        boolean incomplete = result.incompleteTracking();
        record.setTrackingVersion(incomplete ? 2 : 1);
        record.setEndTime(incomplete ? null : new Date());
        record.setDurationMs(incomplete ? null : elapsedMillis(startedNanos));
        record.setDuration(incomplete ? 0L : record.getDurationMs() / 1000);
        record.setStatus(incomplete ? JobLogStatusEnum.UNCONFIRMED
                : failure == null ? JobLogStatusEnum.DONE : JobLogStatusEnum.ERROR);
        String summary = incomplete ? "包含未支持的异步入口，无法确认业务完成" : null;
        if (failure != null) {
            String message = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
            summary = summary == null ? message : summary + "；已观察异常：" + message;
        }
        if (summary != null) record.setErrMsg(boundSummary(summary));
        if (executionLog != null) {
            String message = incomplete ? summary
                    : "任务执行" + (failure == null ? "成功" : "失败") + "，总耗时 " + record.getDurationMs() + " 毫秒";
            executionLog.finish(message, failure);
        }
        // 先关闭文件再发布终态，轮询才能读到稳定 EOF；事务内条件更新保证只累计一次。
        try {
            if (jobLogBiz.finishExecution(record) && onFinished != null) onFinished.accept(record);
        } catch (Exception e) {
            log.error("登记任务最终结果失败，执行记录：{}", record.getId(), e);
        }
    }

    private static long elapsedMillis(long startedNanos) {
        return Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos));
    }

    private static String boundSummary(String summary) {
        int end = Math.min(summary.length(), 2048);
        if (end < summary.length() && end > 0 && Character.isHighSurrogate(summary.charAt(end - 1))) end--;
        return summary.substring(0, end);
    }

    protected abstract void run();

    protected JobLog createLog(int jobId) {
        JobLog jobLog = new JobLog();
        jobLog.setJobId(jobId);
        jobLog.setBeginTime(new Date());
        jobLog.setStatus(JobLogStatusEnum.DOING);
        jobLog.setDuration(0L);
        jobLog.setTrackingVersion(1);
        jobLogBiz.beginExecution(jobLog);
        return jobLog;
    }

    protected String printNextFireTime(JobExecutionContext context) {
        if (context.getNextFireTime() != null) {
            return this.getClass().getSimpleName() + "：下次执行时间=====" +
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                            .format(context.getNextFireTime()) + "=======================";
        }
        return this.getClass().getSimpleName() + "：单次执行结束=======================";
    }

}
