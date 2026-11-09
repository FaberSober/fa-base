package com.faber.config.quartz.customer;

import com.faber.config.quartz.BaseJob;
import com.faber.config.quartz.logging.JobRunLogger;
import com.faber.core.annotation.FaJob;

/** 无业务副作用，供页面验证流程并发和依赖。 */
@FaJob("流程演示：等待3秒")
public class JobFlowDemoWait extends BaseJob {
    @Override protected void run() {
        JobRunLogger.info("演示等待开始，3秒后成功");
        pause(3000);
        JobRunLogger.info("演示等待完成");
    }
    static void pause(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("流程演示等待被中断", error);
        }
    }
}
