package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.api.base.admin.mapper.JobLogMapper;
import com.faber.api.base.admin.mapper.JobMapper;
import com.faber.config.quartz.logging.JobLogFileService;
import com.faber.core.constant.FaSetting;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JobLogBizTest {
    @TempDir Path directory;
    private JobLogBiz biz;
    private JobMapper jobMapper;
    private JobLogMapper mapper;
    private JobLogFileService files;

    @BeforeEach
    void setUp() {
        FaSetting setting = new FaSetting();
        setting.getJobLog().setRootPath(directory.toString());
        files = new JobLogFileService(setting);
        biz = spy(new JobLogBiz());
        jobMapper = mock(JobMapper.class);
        mapper = mock(JobLogMapper.class);
        ReflectionTestUtils.setField(biz, "jobMapper", jobMapper);
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);
        ReflectionTestUtils.setField(biz, "logFiles", files);
        ReflectionTestUtils.setField(biz, "faSetting", setting);
    }

    @Test
    void disabledOrHistoricalTaskDoesNotCreateFile() {
        Job job = new Job();
        when(jobMapper.selectById(12)).thenReturn(job);
        assertNull(biz.openExecutionLog(record(1, JobLogStatusEnum.DOING)));
        verify(biz, never()).updateById(any(JobLog.class));
    }

    @Test
    void capturesEnabledFlagOnceAndPersistsOnlyRelativePath() throws Exception {
        Job job = new Job();
        job.setDetailLogEnabled(true);
        when(jobMapper.selectById(12)).thenReturn(job);
        doReturn(true).when(biz).updateById(any(JobLog.class));
        JobLog record = record(1, JobLogStatusEnum.DOING);
        try (var execution = biz.openExecutionLog(record)) {
            assertNotNull(execution);
            assertFalse(Path.of(record.getLogPath()).isAbsolute());
            job.setDetailLogEnabled(false);
            execution.append("INFO", "已启动的执行继续记录", null);
            assertTrue(Files.readString(files.resolve(record.getLogPath())).contains("已启动的执行继续记录"));
        }
        verify(biz, times(1)).updateById(record);
    }

    @Test
    void fileInitializationFailureDoesNotEscapeToBusinessTask() throws Exception {
        Job job = new Job();
        job.setDetailLogEnabled(true);
        when(jobMapper.selectById(12)).thenReturn(job);
        FaSetting setting = new FaSetting();
        Path notADirectory = directory.resolve("file");
        Files.writeString(notADirectory, "占用根目录");
        setting.getJobLog().setRootPath(notADirectory.toString());
        ReflectionTestUtils.setField(biz, "logFiles", new JobLogFileService(setting));
        JobLog record = record(1, JobLogStatusEnum.DOING);
        assertNull(biz.openExecutionLog(record));
        assertNull(record.getLogPath());
    }

    @Test
    void readUsesStatusSnapshotBeforeAccessingFile() throws Exception {
        JobLog record = record(1, JobLogStatusEnum.DONE);
        record.setLogPath("2026/10/30/12/1.log");
        JobLogFileService mockFiles = mock(JobLogFileService.class);
        when(mockFiles.read(record.getLogPath(), 0, 1024, true))
                .thenReturn(new com.faber.api.base.admin.vo.ret.JobLogReadVo());
        ReflectionTestUtils.setField(biz, "logFiles", mockFiles);
        doReturn(record).when(biz).getById(1);
        assertEquals(JobLogStatusEnum.DONE, biz.readLog(1, 0, 1024).getStatus());
        var order = inOrder(biz, mockFiles);
        order.verify(biz).getById(1);
        order.verify(mockFiles).read(record.getLogPath(), 0, 1024, true);
    }

    @Test
    void runningSingleOrBatchDeletionIsRejectedBeforeAnyDelete() {
        JobLog running = record(1, JobLogStatusEnum.DOING);
        doReturn(running).when(biz).getById(1);
        doReturn(List.of(record(2, JobLogStatusEnum.DONE), running)).when(biz).listByIds(any());
        assertThrows(BuzzException.class, () -> biz.removeById(1));
        assertThrows(BuzzException.class, () -> biz.removeBatchByIds(List.<Serializable>of(2, 1)));
        verifyNoInteractions(mapper);
    }

    @Test
    void completedSingleDeletionAlsoDeletesItsFile() throws Exception {
        JobLog record = record(1, JobLogStatusEnum.DONE);
        try (var execution = files.open(record)) { record.setLogPath(execution.getRelativePath()); }
        doReturn(record).when(biz).getById(1);
        when(mapper.deleteById((Serializable) 1)).thenReturn(1);
        assertTrue(biz.removeById(1));
        assertFalse(Files.exists(files.resolve(record.getLogPath())));
    }

    @Test
    void completedBatchDeletionCleansEveryFileIncludingPermanentDeleteEntry() throws Exception {
        JobLog first = record(1, JobLogStatusEnum.DONE);
        JobLog second = record(2, JobLogStatusEnum.ERROR);
        for (JobLog record : List.of(first, second)) {
            try (var execution = files.open(record)) { record.setLogPath(execution.getRelativePath()); }
        }
        doReturn(List.of(first, second)).when(biz).listByIds(any());
        biz.removePerBatchByIds(List.<Serializable>of(1, 2));
        assertFalse(Files.exists(files.resolve(first.getLogPath())));
        assertFalse(Files.exists(files.resolve(second.getLogPath())));
    }

    @Test
    void downloadWritesFileContentsAndReportsExpiredFileClearly() throws Exception {
        JobLog record = record(1, JobLogStatusEnum.DONE);
        try (var execution = files.open(record)) {
            record.setLogPath(execution.getRelativePath());
            execution.append("INFO", "可下载的中文日志", null);
        }
        doReturn(record).when(biz).getById(1);
        MockHttpServletResponse response = new MockHttpServletResponse();
        biz.downloadLog(1, response);
        assertEquals(Files.readString(files.resolve(record.getLogPath())), response.getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals("job-1.log", response.getHeader("Fa-Filename"));
        files.delete(record.getLogPath());
        assertThrows(BuzzException.class, () -> biz.downloadLog(1, new MockHttpServletResponse()));
    }

    @Test
    void cleanupUsesEndTimeRatherThanDateDirectoryAndPreservesRunningFiles() throws Exception {
        Instant old = Instant.now().minus(40, ChronoUnit.DAYS);
        JobLog running = record(1, JobLogStatusEnum.DOING);
        JobLog recent = record(2, JobLogStatusEnum.DONE);
        recent.setEndTime(new Date());
        JobLog expired = record(3, JobLogStatusEnum.ERROR);
        expired.setEndTime(Date.from(old));
        for (JobLog record : List.of(running, recent, expired)) {
            try (var execution = files.open(record)) { record.setLogPath(execution.getRelativePath()); }
            Files.setLastModifiedTime(files.resolve(record.getLogPath()), FileTime.from(old));
            doReturn(record).when(biz).getById(record.getId());
        }
        assertEquals(1, biz.cleanupExpiredFiles());
        assertTrue(Files.exists(files.resolve(running.getLogPath())));
        assertTrue(Files.exists(files.resolve(recent.getLogPath())));
        assertFalse(Files.exists(files.resolve(expired.getLogPath())));
    }

    @Test
    void newMysqlAndPostgreSqlMigrationsPassRealHeaderParser() throws Exception {
        SystemUpdateLogBiz parser = new SystemUpdateLogBiz();
        for (String dialect : List.of("mysql", "postgre")) {
            var resource = new ClassPathResource("sql/fa-base/" + dialect + "/1.0.52_base_job_file_log.sql");
            var header = parser.getSqlFileHeader(resource);
            assertEquals(1_000_052L, header.getVer());
            assertEquals("V1.0.52", header.getVerNo());
        }
    }

    private JobLog record(int id, JobLogStatusEnum status) {
        JobLog record = new JobLog();
        record.setId(id);
        record.setJobId(12);
        record.setBeginTime(new Date());
        record.setStatus(status);
        return record;
    }
}
