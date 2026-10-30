package com.faber.config.quartz.logging;

import com.faber.api.base.admin.entity.JobLog;
import com.faber.core.constant.FaSetting;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class JobLogFileServiceTest {
    @TempDir Path directory;
    private FaSetting setting;
    private JobLogFileService files;

    @BeforeEach
    void setUp() {
        setting = new FaSetting();
        setting.getJobLog().setRootPath(directory.toString());
        files = new JobLogFileService(setting);
    }

    @Test
    void isolatesExecutionsAndUsesBeginDateEvenWhenWritingLater() throws Exception {
        try (var first = files.open(record(1)); var second = files.open(record(2))) {
            assertEquals("2026/10/30/12/1.log", first.getRelativePath());
            first.append("INFO", "第一个任务", null);
            second.append("WARN", "第二个任务", null);
            assertTrue(Files.readString(files.resolve(first.getRelativePath())).contains("第一个任务"));
            assertFalse(Files.readString(files.resolve(first.getRelativePath())).contains("第二个任务"));
        }
    }

    @Test
    void readsUtf8IncludingEmojiAtSmallByteBoundariesWithoutLoss() throws Exception {
        String text = "开始😀中文\n".repeat(25);
        String relative;
        try (var execution = files.open(record(1))) { relative = execution.getRelativePath(); }
        Files.writeString(files.resolve(relative), text);
        StringBuilder collected = new StringBuilder();
        long offset = 0;
        for (int i = 0; i < 1000; i++) {
            var chunk = files.read(relative, offset, 4, true);
            collected.append(chunk.getContent());
            offset = Long.parseLong(chunk.getNextOffset());
            if (!chunk.isHasMore()) break;
            assertFalse(chunk.getContent().isEmpty(), "完整字符必须推动游标前进");
        }
        assertEquals(text, collected.toString());
        assertEquals(text.getBytes(StandardCharsets.UTF_8).length, offset);
        assertThrows(BuzzException.class, () -> files.read(relative, -1, 1024, false));
    }

    @Test
    void waitsForIncompleteUtf8WhileRunningAndConsumesBrokenTailWhenFinished() throws Exception {
        String relative;
        try (var execution = files.open(record(1))) { relative = execution.getRelativePath(); }
        byte[] emoji = "😀".getBytes(StandardCharsets.UTF_8);
        Files.write(files.resolve(relative), new byte[]{emoji[0], emoji[1]});
        var partial = files.read(relative, 0, 4, false);
        assertEquals("", partial.getContent());
        assertEquals("0", partial.getNextOffset());
        assertTrue(partial.isHasMore());
        Files.write(files.resolve(relative), new byte[]{emoji[2], emoji[3]}, StandardOpenOption.APPEND);
        assertEquals("😀", files.read(relative, 0, 4, false).getContent());
        Files.write(files.resolve(relative), new byte[]{emoji[0], emoji[1]});
        var terminal = files.read(relative, 0, 4, true);
        assertEquals("2", terminal.getNextOffset());
        assertFalse(terminal.isHasMore());
    }

    @Test
    void truncatesWithinCapAndReservesFinalFailureSummary() throws Exception {
        setting.getJobLog().setMaxFileSize(DataSize.ofBytes(1024));
        var execution = files.open(record(1));
        execution.append("INFO", "中文😀".repeat(5000), null);
        execution.append("INFO", "后续正文不应写入", null);
        execution.finish("任务执行失败", new IllegalStateException("模拟异常"));
        execution.append("INFO", "关闭后不应写入", null);
        Path path = files.resolve(execution.getRelativePath());
        String text = Files.readString(path);
        assertTrue(Files.size(path) <= 1024);
        assertTrue(text.contains("日志已达到大小上限"));
        assertTrue(text.contains("任务执行失败"));
        assertFalse(text.contains("后续正文不应写入"));
        assertFalse(text.contains("关闭后不应写入"));
        assertFalse(text.contains("�"));
    }

    @Test
    void propagatesAndRestoresContextOnReusedWorkerThread() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var execution = files.open(record(1))) {
            try (var ignored = JobRunLogger.bind(execution)) {
                JobRunLogger.info("父任务 {}", 1);
                executor.submit(JobRunLogger.wrap(() -> JobRunLogger.info("子任务 {}", 2))).get();
            }
            executor.submit(() -> JobRunLogger.info("线程复用不应串入")).get();
            JobRunLogger.info("父线程已清理");
            String text = Files.readString(files.resolve(execution.getRelativePath()));
            assertTrue(text.contains("父任务 1"));
            assertTrue(text.contains("子任务 2"));
            assertFalse(text.contains("线程复用不应串入"));
            assertFalse(text.contains("父线程已清理"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void cleanupPreservesActiveAndUnapprovedFilesAndRemovesEmptyDirectories() throws Exception {
        Instant cutoff = Instant.now().minusSeconds(3600);
        var active = files.open(record(1));
        var finished = files.open(record(2));
        var recentEnd = files.open(record(3));
        finished.close();
        recentEnd.close();
        for (var execution : new JobLogFileService.ExecutionLog[]{active, finished, recentEnd}) {
            Files.setLastModifiedTime(files.resolve(execution.getRelativePath()), FileTime.from(cutoff.minusSeconds(3600)));
        }
        assertEquals(1, files.cleanup(cutoff, (id, path) -> id != 3));
        assertTrue(Files.exists(files.resolve(active.getRelativePath())));
        assertFalse(Files.exists(files.resolve(finished.getRelativePath())));
        assertTrue(Files.exists(files.resolve(recentEnd.getRelativePath())));
        active.close();
        assertEquals(2, files.cleanup(cutoff, (id, path) -> true));
        assertFalse(Files.exists(directory.resolve("2026")));
        assertTrue(Files.exists(directory));
    }

    @Test
    void rejectsEscapingPathsAndHandlesMissingFiles() throws Exception {
        assertThrows(BuzzException.class, () -> files.resolve("../outside.log"));
        assertThrows(BuzzException.class, () -> files.resolve(directory.resolveSibling("outside.log").toString()));
        assertFalse(files.read(null, 0, 1024, true).isAvailable());
        assertFalse(files.read("2026/10/30/12/404.log", 0, 1024, true).isAvailable());
        files.delete("2026/10/30/12/404.log");
    }

    private JobLog record(int id) {
        JobLog record = new JobLog();
        record.setId(id);
        record.setJobId(12);
        record.setBeginTime(Date.from(LocalDateTime.of(2026, 10, 30, 23, 59).atZone(ZoneId.systemDefault()).toInstant()));
        return record;
    }
}
