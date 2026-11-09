package com.faber.config.quartz;

import com.faber.api.base.admin.flow.JobFlowRunner;
import com.faber.core.context.BaseContextHandler;
import jakarta.annotation.Resource;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/** Quartz 仅负责触发；业务异步生命周期由流程执行器持有。 */
public class JobFlowQuartzJob implements Job {
    @Resource private JobFlowRunner runner;
    @Override public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            BaseContextHandler.useAdmin();
            runner.startScheduled(context.getMergedJobDataMap().getInt("jobId"));
        } catch (Exception error) { throw new JobExecutionException(error, false); }
        finally { BaseContextHandler.remove(); }
    }
}
