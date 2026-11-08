package com.faber.config.quartz;

import com.faber.api.base.admin.biz.JobLogBiz;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.core.config.thread.TaskExecutionTracker;
import com.faber.core.config.thread.TrackingThreadPoolTaskExecutor;
import com.faber.core.context.BaseContextHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Date;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BaseJobFlowLifecycleTest {
    private JobLog record() {
        JobLog log = new JobLog(); log.setId(71); log.setBeginTime(new Date()); log.setStatus(JobLogStatusEnum.DOING); log.setTrackingVersion(1); return log;
    }
    private BaseJob job(JobLogBiz biz, Runnable work) {
        BaseJob job = new BaseJob() { @Override protected void run() { work.run(); } };
        ReflectionTestUtils.setField(job, "jobLogBiz", biz); return job;
    }
    @Test void callbackWaitsForRealAsyncCompletionAndPublishedTransaction() throws Exception {
        JobLogBiz biz = mock(JobLogBiz.class); when(biz.finishExecution(any())).thenReturn(true);
        TrackingThreadPoolTaskExecutor executor = new TrackingThreadPoolTaskExecutor(); executor.setCorePoolSize(1); executor.initialize();
        CountDownLatch release = new CountDownLatch(1); CompletableFuture<JobLog> finished = new CompletableFuture<>();
        JobLog record = record(); BaseContextHandler.setUserId("original");
        try {
            job(biz, () -> executor.execute(() -> { try { release.await(5, TimeUnit.SECONDS); } catch (InterruptedException e) { throw new RuntimeException(e); } }))
                    .executeFlowNode(record, false, value -> { verify(biz).finishExecution(value); assertNull(TaskExecutionTracker.current()); finished.complete(value); });
            assertEquals("original", BaseContextHandler.getUserId()); assertFalse(finished.isDone()); assertEquals(JobLogStatusEnum.DOING, record.getStatus());
            release.countDown();
            assertEquals(JobLogStatusEnum.DONE, finished.get(5, TimeUnit.SECONDS).getStatus());
            assertNull(record.getJobId()); verify(biz, never()).beginExecution(any());
        } finally { release.countDown(); executor.shutdown(); BaseContextHandler.remove(); }
    }
    @Test void failedPublicationNeverAdvancesFlow() {
        JobLogBiz biz = mock(JobLogBiz.class); when(biz.finishExecution(any())).thenThrow(new IllegalStateException("db"));
        var callback = new java.util.concurrent.atomic.AtomicBoolean();
        job(biz, () -> {}).executeFlowNode(record(), false, value -> callback.set(true));
        assertFalse(callback.get());
    }
    @Test void synchronousFailurePublishesFailureBeforeCallback() {
        JobLogBiz biz = mock(JobLogBiz.class); when(biz.finishExecution(any())).thenReturn(true);
        JobLog record = record();
        job(biz, () -> { throw new IllegalStateException("business"); }).executeFlowNode(record, false, value -> {
            verify(biz).finishExecution(value); assertEquals(JobLogStatusEnum.ERROR, value.getStatus()); assertEquals("business", value.getErrMsg());
        });
        assertEquals(JobLogStatusEnum.ERROR, record.getStatus());
    }
}
