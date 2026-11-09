package com.faber.api.base.admin.jobs;

import com.faber.api.base.admin.biz.LogArchiveBiz;
import com.faber.config.quartz.BaseJob;
import com.faber.core.annotation.FaJob;

import jakarta.annotation.Resource;

/** 清理过期的请求日志归档表。 */
@FaJob(value = "清理过期请求日志归档", group = "日志维护", description = "清理超过保留期限的请求日志归档", tags = {"清理"})
public class JobLogArchiveCleanup extends BaseJob {

    @Resource private LogArchiveBiz logArchiveBiz;

    @Override
    protected void run() {
        logArchiveBiz.cleanExpiredArchives();
    }
}
