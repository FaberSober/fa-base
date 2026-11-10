package com.faber.api.base.admin.flow;

import com.faber.api.base.admin.entity.JobLog;
import com.faber.config.quartz.logging.JobLogFileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 文件日志为尽力记录，持久运行状态才是事实来源；只记录已提交的编排动作。 */
@Slf4j
@Service
public class JobFlowEventLog {
    @Resource private JobLogFileService files;

    public void open(JobLog record, boolean enabled) {
        if (!enabled) return;
        try (var file = files.open(record)) {
            record.setLogPath(file.getRelativePath());
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) files.delete(file.getRelativePath());
                    }
                });
            }
        } catch (Exception error) {
            record.setLogPath(null);
            log.warn("初始化流程编排日志失败，运行ID：{}", record.getId(), error);
        }
    }

    public void record(JobLog parent, String message, boolean terminal) {
        String path = parent.getLogPath();
        if (path == null || path.isBlank()) return;
        Runnable append = () -> {
            try { files.appendFlowEvent(path, message, terminal); }
            catch (Exception error) { log.warn("写入流程编排日志失败，运行ID：{}", parent.getId(), error); }
        };
        Runnable release = files.retainFlowEvent(path);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { append.run(); }
                @Override public void afterCompletion(int status) { release.run(); }
            });
        } else {
            try { append.run(); } finally { release.run(); }
        }
    }

    public void record(JobLog parent, String message) { record(parent, message, false); }
}
