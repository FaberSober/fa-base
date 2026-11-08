package com.faber.config.quartz.logging;

import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.vo.ret.JobLogReadVo;
import com.faber.core.constant.FaSetting;
import com.faber.core.exception.BuzzException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

/** 文件正文不入库，每个执行记录独占一个有大小上限的文件。 */
@Slf4j
@Service
public class JobLogFileService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final FaSetting setting;
    private final Set<Path> activeFiles = ConcurrentHashMap.newKeySet();

    public JobLogFileService(FaSetting setting) {
        this.setting = setting;
    }

    public ExecutionLog open(JobLog record) throws IOException {
        long maxBytes = setting.getJobLog().getMaxFileSize().toBytes();
        if (maxBytes < 1024) throw new BuzzException("任务日志文件上限不能小于 1 KiB");
        String date = DATE.format(record.getBeginTime().toInstant().atZone(ZoneId.systemDefault()));
        String relativePath = date + "/" + (record.getJobId() == null ? record.getId() : record.getJobId()) + "/" + record.getId() + ".log";
        Path path = resolve(relativePath);
        Files.createDirectories(path.getParent());
        // 创建目录后再次检查，防止将日志写到根目录之外的符号链接。
        path = resolve(relativePath);
        OutputStream output = new BufferedOutputStream(Files.newOutputStream(path,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE), 8192);
        activeFiles.add(path);
        return new ExecutionLog(relativePath, path, output, maxBytes);
    }

    public JobLogReadVo read(String relativePath, long offset, int maxBytes, boolean finished) throws IOException {
        if (offset < 0) throw new BuzzException("日志读取位置不能为负数");
        JobLogReadVo result = new JobLogReadVo();
        if (relativePath == null || relativePath.isBlank()) {
            result.setMessage("本次执行未记录详细日志");
            return result;
        }
        Path path = resolve(relativePath);
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
            result.setMessage("日志已清理或不可用");
            return result;
        }
        int limit = Math.max(4, Math.min(maxBytes, 64 * 1024));
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
            long size = channel.size();
            if (offset > size) throw new BuzzException("日志读取位置已失效，请重新加载");
            channel.position(offset);
            ByteBuffer buffer = ByteBuffer.allocate((int) Math.min(limit, size - offset));
            while (buffer.hasRemaining() && channel.read(buffer) > 0) { }
            int length = completeUtf8Length(buffer.array(), buffer.position());
            // 已结束文件若存在不完整尾字符，消费并替换它，避免游标永久停滞。
            if (finished && offset + buffer.position() == size) length = buffer.position();
            result.setAvailable(true);
            result.setContent(new String(buffer.array(), 0, length, StandardCharsets.UTF_8));
            result.setNextOffset(Long.toString(offset + length));
            result.setHasMore(offset + length < size);
            return result;
        }
    }

    public Path resolve(String relativePath) throws IOException {
        String configuredRoot = setting.getJobLog().getRootPath();
        if (configuredRoot == null || configuredRoot.isBlank()) throw new BuzzException("任务日志根目录未配置");
        Path root = Path.of(configuredRoot).toAbsolutePath().normalize();
        Path relative = Path.of(relativePath);
        Path target = root.resolve(relative).normalize();
        if (relative.isAbsolute() || !target.startsWith(root) || target.equals(root)) {
            throw new BuzzException("任务日志路径无效");
        }
        for (Path current = target; current != null && current.startsWith(root); current = current.getParent()) {
            if (Files.isSymbolicLink(current)) throw new BuzzException("任务日志路径不允许符号链接");
        }
        return target;
    }

    public void delete(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) return;
        try {
            Path path = resolve(relativePath);
            if (!activeFiles.contains(path)) Files.deleteIfExists(path);
        } catch (IOException | RuntimeException e) {
            log.error("清理任务日志文件失败：{}", relativePath, e);
        }
    }

    /** 只检查过期文件；由业务层确认对应记录已结束，或文件已成为孤立文件。 */
    public int cleanup(Instant cutoff, BiPredicate<Integer, String> canDelete) throws IOException {
        Path root = Path.of(setting.getJobLog().getRootPath()).toAbsolutePath().normalize();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) return 0;
        int[] deleted = {0};
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                if (!attrs.isRegularFile() || activeFiles.contains(file) || !attrs.lastModifiedTime().toInstant().isBefore(cutoff)
                        || !relative.matches("\\d{4}/\\d{2}/\\d{2}/\\d+/\\d+\\.log")) return FileVisitResult.CONTINUE;
                String filename = file.getFileName().toString();
                try {
                    int id = Integer.parseInt(filename.substring(0, filename.length() - 4));
                    if (canDelete.test(id, relative) && Files.deleteIfExists(resolve(relative))) deleted[0]++;
                } catch (IOException e) {
                    log.error("清理过期任务日志失败：{}", relative, e);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException error) throws IOException {
                if (error != null) throw error;
                if (!dir.equals(root)) {
                    try {
                        Files.delete(dir);
                    } catch (DirectoryNotEmptyException | NoSuchFileException ignored) {
                        // 目录可能仍有有效日志，或正被另一个清理进程处理。
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return deleted[0];
    }

    private static int completeUtf8Length(byte[] data, int length) {
        if (length == 0) return 0;
        int start = length - 1;
        while (start > 0 && (data[start] & 0xc0) == 0x80) start--;
        int lead = data[start] & 0xff;
        int needed = lead < 0x80 ? 1 : lead < 0xe0 ? 2 : lead < 0xf0 ? 3 : 4;
        return length - start < needed ? start : length;
    }

    private static byte[] format(String level, String message, Throwable error) {
        StringBuilder text = new StringBuilder(TIME.format(LocalDateTime.now()))
                .append(' ').append(level).append("  ").append(message).append('\n');
        if (error != null) {
            StringWriter stack = new StringWriter();
            error.printStackTrace(new PrintWriter(stack));
            text.append(stack);
        }
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    public final class ExecutionLog implements AutoCloseable {
        private final String relativePath;
        private final Path path;
        private final OutputStream output;
        private final long maxBytes;
        private final long bodyLimit;
        private final long footerReserve;
        private long written;
        private boolean truncated;
        private boolean closed;

        private ExecutionLog(String relativePath, Path path, OutputStream output, long maxBytes) {
            this.relativePath = relativePath;
            this.path = path;
            this.output = output;
            this.maxBytes = maxBytes;
            this.footerReserve = Math.min(8192, maxBytes / 4);
            this.bodyLimit = maxBytes - footerReserve - 256;
        }

        public String getRelativePath() {
            return relativePath;
        }

        public synchronized void append(String level, String message, Throwable error) {
            if (closed || truncated) return;
            try {
                byte[] bytes = format(level, message, error);
                long remaining = bodyLimit - written;
                if (bytes.length > remaining) {
                    byte[] marker = format("WARN", "日志已达到大小上限，后续正文已省略。", null);
                    writeBounded(bytes, remaining);
                    output.write('\n');
                    written++;
                    writeBounded(marker, maxBytes - footerReserve - written);
                    truncated = true;
                } else {
                    writeBounded(bytes, remaining);
                }
                output.flush();
            } catch (IOException e) {
                log.error("写入任务日志失败：{}", relativePath, e);
                close();
            }
        }

        /** 结束信息使用预留空间，刷新关闭后调用方才能发布执行终态。 */
        public synchronized void finish(String message, Throwable error) {
            if (closed) return;
            try {
                output.write('\n');
                written++;
                writeBounded(format(error == null ? "INFO" : "ERROR", message, error), maxBytes - written);
            } catch (IOException e) {
                log.error("写入任务结束日志失败：{}", relativePath, e);
            } finally {
                close();
            }
        }

        private void writeBounded(byte[] bytes, long limit) throws IOException {
            int length = completeUtf8Length(bytes, (int) Math.min(bytes.length, Math.max(0, limit)));
            output.write(bytes, 0, length);
            written += length;
        }

        @Override
        public synchronized void close() {
            if (closed) return;
            closed = true;
            try {
                output.close();
            } catch (IOException e) {
                log.error("关闭任务日志失败：{}", relativePath, e);
            } finally {
                activeFiles.remove(path);
            }
        }
    }
}
