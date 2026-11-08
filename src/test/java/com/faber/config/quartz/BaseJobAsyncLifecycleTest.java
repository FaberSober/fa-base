package com.faber.config.quartz;

import com.faber.api.base.admin.biz.JobLogBiz;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.config.quartz.logging.JobLogFileService;
import com.faber.config.quartz.logging.JobRunLogger;
import com.faber.core.config.thread.TrackingThreadPoolTaskExecutor;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BaseJobAsyncLifecycleTest {
    @TempDir Path directory;
    private TrackingThreadPoolTaskExecutor executor;
    private JobLogBiz biz;
    private JobLogFileService files;
    private final AtomicReference<JobLog> record = new AtomicReference<>();
    private final CompletableFuture<JobLog> finished = new CompletableFuture<>();

    @BeforeEach
    void setUp() {
        FaSetting setting = new FaSetting();
        setting.getJobLog().setRootPath(directory.toString());
        files = new JobLogFileService(setting);
        executor = new TrackingThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.initialize();
        biz = mock(JobLogBiz.class);
        doAnswer(invocation -> {
            JobLog value = invocation.getArgument(0);
            value.setId(1);
            record.set(value);
            return null;
        }).when(biz).beginExecution(any());
        when(biz.openExecutionLog(any())).thenAnswer(invocation -> {
            JobLog value = invocation.getArgument(0);
            var execution = files.open(value);
            value.setLogPath(execution.getRelativePath());
            return execution;
        });
        when(biz.finishExecution(any())).thenAnswer(invocation -> {
            JobLog value = invocation.getArgument(0);
            String before = Files.readString(files.resolve(value.getLogPath()));
            JobRunLogger.info("发布终态时日志应已关闭");
            assertEquals(before, Files.readString(files.resolve(value.getLogPath())));
            finished.complete(value);
            return true;
        });
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
        BaseContextHandler.remove();
    }

    @Test
    void returnsToSchedulerWhileChildContinuesAndClosesLogOnlyAfterChild() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        BaseContextHandler.setUserId("caller");
        BaseJob job = job(() -> executor.execute(() -> {
            await(release);
            JobRunLogger.info("异步业务真正结束");
        }));
        try {
            job.execute(context());
            JobLog value = record.get();
            assertEquals("caller", BaseContextHandler.getUserId());
            assertEquals(JobLogStatusEnum.DOING, value.getStatus());
            assertNull(value.getEndTime());
            assertNull(value.getDurationMs());
            assertNotNull(value.getDispatchDurationMs());
            verify(biz).recordDispatch(value);
            assertFalse(finished.isDone());
            assertFalse(Files.readString(files.resolve(value.getLogPath())).contains("任务执行成功"));
        } finally {
            release.countDown();
        }
        JobLog value = finished.get(5, TimeUnit.SECONDS);
        assertEquals(JobLogStatusEnum.DONE, value.getStatus());
        assertEquals(1, value.getTrackingVersion());
        assertNotNull(value.getEndTime());
        assertTrue(value.getDurationMs() >= value.getDispatchDurationMs());
        String text = Files.readString(files.resolve(value.getLogPath()));
        assertTrue(text.contains("异步业务真正结束"));
        assertTrue(text.contains("任务执行成功"));
        verify(biz, times(1)).finishExecution(value);
    }

    @Test
    void childFailureBecomesWholeExecutionFailureAndIsWrittenToLog() throws Exception {
        job(() -> executor.execute(() -> {
            throw new IllegalStateException("异步业务失败");
        })).execute(context());
        JobLog value = finished.get(5, TimeUnit.SECONDS);
        assertEquals(JobLogStatusEnum.ERROR, value.getStatus());
        assertEquals("异步业务失败", value.getErrMsg());
        assertNotNull(value.getDurationMs());
        assertTrue(Files.readString(files.resolve(value.getLogPath())).contains("异步业务失败"));
        verify(biz, times(1)).finishExecution(value);
    }

    @Test
    void futureWithInternallyCapturedFailureIsUnconfirmedInsteadOfSuccessful() throws Exception {
        job(() -> CompletableFuture.runAsync(() -> {
            throw new IllegalStateException("Future内部保存异常");
        }, executor)).execute(context());
        JobLog value = finished.get(5, TimeUnit.SECONDS);
        assertEquals(JobLogStatusEnum.UNCONFIRMED, value.getStatus());
        assertEquals(2, value.getTrackingVersion());
        assertNull(value.getEndTime());
        assertNull(value.getDurationMs());
        assertTrue(value.getErrMsg().contains("无法确认业务完成"));
        assertFalse(Files.readString(files.resolve(value.getLogPath())).contains("任务执行成功"));
    }

    @Test
    void statisticsFailureDoesNotRerunOrFailCompletedBusiness() throws Exception {
        AtomicReference<Boolean> invoked = new AtomicReference<>(false);
        doThrow(new IllegalStateException("统计数据库故障")).when(biz).finishExecution(any());
        job(() -> {
            assertFalse(invoked.getAndSet(true));
            JobRunLogger.info("业务已完成");
        }).execute(context());
        assertTrue(invoked.get());
        assertEquals(JobLogStatusEnum.DONE, record.get().getStatus());
        verify(biz, times(1)).finishExecution(record.get());
        assertTrue(Files.readString(files.resolve(record.get().getLogPath())).contains("任务执行成功"));
    }

    private BaseJob job(Runnable body) {
        BaseJob job = new BaseJob() {
            @Override protected void run() { body.run(); }
        };
        ReflectionTestUtils.setField(job, "jobLogBiz", biz);
        return job;
    }

    private JobExecutionContext context() {
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getJobDetail()).thenReturn(JobBuilder.newJob(com.faber.config.quartz.customer.JobDemo1.class)
                .usingJobData("jobId", 12).build());
        return context;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("等待测试信号超时");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(error);
        }
    }
}
