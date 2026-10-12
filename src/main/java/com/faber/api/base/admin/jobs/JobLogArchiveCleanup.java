package com.faber.api.base.admin.jobs;

import com.faber.api.base.admin.biz.LogArchiveBiz;
import com.faber.config.quartz.BaseJob;
import com.faber.core.annotation.FaJob;

import jakarta.annotation.Resource;

/** 清理过期的请求日志归档表。 */
@FaJob("清理过期请求日志归档")
public class JobLogArchiveCleanup extends BaseJob {

    @Resource private LogArchiveBiz logArchiveBiz;

    @Override
    protected void run() {
        logArchiveBiz.cleanExpiredArchives();
    }
}
