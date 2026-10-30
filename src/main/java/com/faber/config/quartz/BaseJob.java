package com.faber.config.quartz;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.faber.api.base.admin.biz.JobLogBiz;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.core.context.BaseContextHandler;
import com.faber.config.quartz.logging.JobLogFileService;
import com.faber.config.quartz.logging.JobRunLogger;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import jakarta.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.Date;

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
        int jobId = context.getJobDetail().getJobDataMap().getIntValue("jobId");
        JobLog jobLog = this.createLog(jobId);
        JobLogFileService.ExecutionLog executionLog = null;
        JobRunLogger.Scope scope = null;
        Throwable failure = null;
        try {
            // 定时任务是以admin角色来执行的
            BaseContextHandler.useAdmin();
            executionLog = jobLogBiz.openExecutionLog(jobLog);
            scope = JobRunLogger.bind(executionLog);
            JobRunLogger.info("任务开始：{}，执行记录：{}", getClass().getSimpleName(), jobLog.getId());
            run();
        } catch (Throwable e) {
            failure = e;
            log.error(e.getMessage(), e);
            if (e instanceof Error error) throw error;
        } finally {
            jobLog.setEndTime(new Date());
            jobLog.setDuration(DateUtil.between(jobLog.getEndTime(), jobLog.getBeginTime(), DateUnit.SECOND));
            if (executionLog != null) {
                executionLog.finish("任务执行" + (failure == null ? "成功" : "失败") + "，耗时 " + jobLog.getDuration() + " 秒", failure);
            }
            if (scope != null) scope.close();
            // 文件已经收尾后才能发布终态，前端此后可读到稳定的 EOF。
            jobLog.setStatus(failure == null ? JobLogStatusEnum.DONE : JobLogStatusEnum.ERROR);
            if (failure != null) {
                String summary = failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
                int end = Math.min(summary.length(), 2048);
                if (end < summary.length() && end > 0 && Character.isHighSurrogate(summary.charAt(end - 1))) end--;
                jobLog.setErrMsg(summary.substring(0, end));
            }
            try {
                jobLogBiz.updateById(jobLog);
            } finally {
                BaseContextHandler.remove();
            }
        }

        log.debug(printNextFireTime(context));
    }

    protected abstract void run();

    protected JobLog createLog(int jobId) {
        JobLog jobLog = new JobLog();
        jobLog.setJobId(jobId);
        jobLog.setBeginTime(new Date());
        jobLog.setStatus(JobLogStatusEnum.DOING);
        jobLog.setDuration(0L);
        jobLogBiz.save(jobLog);
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
