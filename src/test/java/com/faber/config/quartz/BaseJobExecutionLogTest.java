package com.faber.config.quartz;

import com.faber.api.base.admin.biz.JobLogBiz;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.config.quartz.logging.JobLogFileService;
import com.faber.config.quartz.logging.JobRunLogger;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BaseJobExecutionLogTest {
    @TempDir Path directory;

    @Test
    void publishesSuccessOnlyAfterFlushingFinalLogAndClearingContext() throws Exception {
        executeAndVerify(null);
    }

    @Test
    void preservesExceptionStackInFileAndBoundsDatabaseSummary() throws Exception {
        executeAndVerify(new IllegalStateException("失败".repeat(2000)));
    }

    @Test
    void fatalErrorIsRecordedAsFailureAndRethrown() throws Exception {
        executeAndVerify(new AssertionError("不可恢复的任务错误"));
    }

    @Test
    void boundedErrorSummaryDoesNotSplitAnEmojiSurrogatePair() throws Exception {
        executeAndVerify(new IllegalArgumentException("a" + "😀".repeat(1500)));
    }

    private void executeAndVerify(Throwable failure) throws Exception {
        FaSetting setting = new FaSetting();
        setting.getJobLog().setRootPath(directory.toString());
        JobLogFileService files = new JobLogFileService(setting);
        JobLog record = new JobLog();
        record.setId(1);
        record.setJobId(12);
        record.setBeginTime(new Date());
        record.setStatus(JobLogStatusEnum.DOING);
        var execution = files.open(record);
        Path path = files.resolve(execution.getRelativePath());
        JobLogBiz biz = mock(JobLogBiz.class);
        when(biz.openExecutionLog(record)).thenReturn(execution);
        when(biz.updateById(any())).thenAnswer(invocation -> {
            String text = Files.readString(path);
            assertTrue(text.contains(failure == null ? "任务执行成功" : "任务执行失败"));
            JobRunLogger.info("终态发布时上下文已经清理");
            assertEquals(text, Files.readString(path));
            return true;
        });
        BaseJob job = new BaseJob() {
            @Override protected JobLog createLog(int jobId) { return record; }
            @Override protected void run() {
                assertEquals(JobLogStatusEnum.DOING, record.getStatus());
                JobRunLogger.info("业务步骤已执行");
                if (failure instanceof Error error) throw error;
                if (failure instanceof RuntimeException exception) throw exception;
            }
        };
        ReflectionTestUtils.setField(job, "jobLogBiz", biz);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getJobDetail()).thenReturn(JobBuilder.newJob(com.faber.config.quartz.customer.JobDemo1.class)
                .usingJobData("jobId", 12).build());
        if (failure instanceof Error) assertThrows(AssertionError.class, () -> job.execute(context));
        else job.execute(context);
        assertEquals(failure == null ? JobLogStatusEnum.DONE : JobLogStatusEnum.ERROR, record.getStatus());
        String text = Files.readString(path);
        assertTrue(text.contains("业务步骤已执行"));
        if (failure != null) {
            assertTrue(record.getErrMsg().length() <= 2048);
            assertFalse(Character.isHighSurrogate(record.getErrMsg().charAt(record.getErrMsg().length() - 1)));
            if (failure instanceof IllegalStateException) assertEquals(2048, record.getErrMsg().length());
            assertTrue(text.contains(failure.getClass().getName()));
        }
        assertNull(BaseContextHandler.getUserId());
        verify(biz, times(1)).updateById(record);
    }
}
