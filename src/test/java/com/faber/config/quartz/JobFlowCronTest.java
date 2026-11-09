package com.faber.config.quartz;

import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.flow.JobFlowRunner;
import com.faber.api.base.admin.flow.JobFlowRunStore;
import com.faber.config.runner.DbJobsStartRunner;
import com.faber.api.base.admin.biz.JobBiz;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import org.junit.jupiter.api.Test;
import org.quartz.*;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobFlowCronTest {
    @Test void adapterRoutesRegisteredIdAndCleansThreadContextOnFailure() throws Exception {
        var runner = mock(JobFlowRunner.class); var adapter = new JobFlowQuartzJob();
        ReflectionTestUtils.setField(adapter, "runner", runner);
        var context = mock(JobExecutionContext.class); var data = new JobDataMap(); data.put("jobId", 42);
        when(context.getMergedJobDataMap()).thenReturn(data);
        adapter.execute(context); verify(runner).startScheduled(42);
        doThrow(new IllegalStateException("database unavailable")).when(runner).startScheduled(42);
        assertThrows(JobExecutionException.class, () -> adapter.execute(context));
        assertNull(BaseContextHandler.getUserId());
    }
    @Test void notReadyRunnerDoesNotCreateRuns() {
        var runner = new JobFlowRunner(); var store = mock(JobFlowRunStore.class);
        ReflectionTestUtils.setField(runner, "store", store);
        runner.startScheduled(42); verifyNoInteractions(store);
    }
    @Test void scheduledQuartzIdentityMatchesRemovalAndOrdinaryClassRemainsUnchanged() throws Exception {
        var scheduler = mock(Scheduler.class); var factory = mock(SchedulerFactoryBean.class);
        when(factory.getScheduler()).thenReturn(scheduler);
        var tasks = new JobTask(); ReflectionTestUtils.setField(tasks, "schedulerFactoryBean", factory);
        Job flow = new Job(); flow.setId(42); flow.setJobType(1); flow.setClazzPath(""); flow.setCron("0 * * * * ?");
        assertTrue(tasks.startJob(flow));
        var details = org.mockito.ArgumentCaptor.forClass(JobDetail.class);
        verify(scheduler).scheduleJob(details.capture(), any(Trigger.class));
        assertEquals(JobFlowQuartzJob.class, details.getValue().getJobClass());
        assertEquals(JobKey.jobKey("42-"), details.getValue().getKey());
        when(scheduler.checkExists(TriggerKey.triggerKey("42-"))).thenReturn(true);
        assertTrue(tasks.remove(flow)); verify(scheduler).deleteJob(details.getValue().getKey());
        clearInvocations(scheduler);
        Job ordinary = new Job(); ordinary.setId(43); ordinary.setClazzPath("com.faber.config.quartz.customer.JobDemo1"); ordinary.setCron(flow.getCron()); ordinary.setJobType(0);
        assertTrue(tasks.startJob(ordinary)); verify(scheduler).scheduleJob(details.capture(), any(Trigger.class));
        assertEquals(ordinary.getClazzPath(), details.getValue().getJobClass().getName());
    }
    @Test void failedSchedulerStartCleansOnlyTheNewRegistration() throws Exception {
        var scheduler = mock(Scheduler.class); var factory = mock(SchedulerFactoryBean.class);
        when(factory.getScheduler()).thenReturn(scheduler);
        var tasks = new JobTask(); ReflectionTestUtils.setField(tasks, "schedulerFactoryBean", factory);
        Job job = new Job(); job.setId(42); job.setJobType(1); job.setClazzPath(""); job.setCron("0 * * * * ?");
        doThrow(new SchedulerException("cannot start")).when(scheduler).start();
        assertFalse(tasks.startJob(job)); verify(scheduler).deleteJob(JobKey.jobKey("42-"));
        clearInvocations(scheduler);
        when(scheduler.scheduleJob(any(JobDetail.class), any(Trigger.class))).thenThrow(new ObjectAlreadyExistsException("existing"));
        assertFalse(tasks.startJob(job)); verify(scheduler, never()).deleteJob(any());
    }
    @Test void bootRecoveryFinishesBeforeCronRegistration() throws Exception {
        var starter = new DbJobsStartRunner(); var biz = mock(JobBiz.class); var tasks = mock(JobTask.class); var runner = mock(JobFlowRunner.class);
        var setting = new FaSetting(); setting.setConfig(new FaSetting.Config()); setting.getConfig().setStartJobsOnBoot(true);
        ReflectionTestUtils.setField(starter, "jobBiz", biz); ReflectionTestUtils.setField(starter, "jobTask", tasks);
        ReflectionTestUtils.setField(starter, "flowRunner", runner); ReflectionTestUtils.setField(starter, "faSetting", setting);
        Job flow = new Job(); when(biz.getStartUpJobs()).thenReturn(java.util.List.of(flow));
        starter.run(); var order = inOrder(runner, tasks); order.verify(runner).recover(); order.verify(tasks).startJob(flow);
    }
}
