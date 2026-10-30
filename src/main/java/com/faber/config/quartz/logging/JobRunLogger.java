package com.faber.config.quartz.logging;

import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

/** 只记录显式业务日志；线程池任务使用 wrap 显式传递并恢复执行上下文。 */
public final class JobRunLogger {
    private static final ThreadLocal<JobLogFileService.ExecutionLog> CURRENT = new ThreadLocal<>();

    private JobRunLogger() { }

    public static void info(String message, Object... arguments) { write("INFO", message, arguments); }
    public static void warn(String message, Object... arguments) { write("WARN", message, arguments); }
    public static void error(String message, Object... arguments) { write("ERROR", message, arguments); }

    private static void write(String level, String message, Object[] arguments) {
        JobLogFileService.ExecutionLog context = CURRENT.get();
        if (context == null) return;
        FormattingTuple text = MessageFormatter.arrayFormat(message, arguments);
        context.append(level, text.getMessage(), text.getThrowable());
    }

    public static Scope bind(JobLogFileService.ExecutionLog context) {
        JobLogFileService.ExecutionLog previous = CURRENT.get();
        if (context == null) CURRENT.remove(); else CURRENT.set(context);
        return new Scope(previous);
    }

    public static Runnable wrap(Runnable task) {
        JobLogFileService.ExecutionLog captured = CURRENT.get();
        return () -> {
            try (Scope ignored = bind(captured)) {
                task.run();
            }
        };
    }

    public static final class Scope implements AutoCloseable {
        private final JobLogFileService.ExecutionLog previous;

        private Scope(JobLogFileService.ExecutionLog previous) { this.previous = previous; }

        @Override
        public void close() {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
}
