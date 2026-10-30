package com.faber.api.base.admin.jobs;

import com.faber.api.base.admin.biz.JobLogBiz;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 文件维护不创建新的任务执行记录，摘要保留期限独立于文件。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobLogFileCleanup {
    private final JobLogBiz jobLogBiz;

    @Scheduled(cron = "${fa.setting.job-log.cleanup-cron:0 0 3 * * ?}")
    public void cleanup() {
        try {
            int count = jobLogBiz.cleanupExpiredFiles();
            if (count > 0) log.info("已清理 {} 个过期任务日志文件", count);
        } catch (Exception e) {
            log.error("定时清理任务日志文件失败", e);
        }
    }
}
