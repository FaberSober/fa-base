package com.faber.api.base.admin.jobs;

import com.faber.api.base.admin.biz.LogArchiveBiz;
import com.faber.config.quartz.BaseJob;
import com.faber.core.annotation.FaJob;

import jakarta.annotation.Resource;

/** 按月归档请求日志。 */
@FaJob(value = "按月归档请求日志", group = "日志维护", description = "按月归档历史请求日志", tags = {"归档"})
public class JobLogArchive extends BaseJob {

    @Resource private LogArchiveBiz logArchiveBiz;

    @Override
    protected void run() {
        logArchiveBiz.archivePreviousMonth();
    }
}
