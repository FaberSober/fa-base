package com.faber.config.quartz.customer;

import com.faber.config.quartz.BaseJob;
import com.faber.config.quartz.logging.JobRunLogger;
import com.faber.core.annotation.FaJob;
import jakarta.annotation.Resource;
import java.util.concurrent.Executor;

/** 使用框架受跟踪线程池，主方法返回后仍须等待异步工作完成。 */
@FaJob(value = "流程演示：异步等待6秒", group = "流程演示", description = "异步等待6秒后成功，主方法返回后继续跟踪", tags = {"异步"})
public class JobFlowDemoAsync extends BaseJob {
    @Resource(name = "executor") private Executor executor;
    @Override protected void run() {
        executor.execute(() -> {
            JobRunLogger.info("演示异步工作开始，6秒后成功");
            JobFlowDemoWait.pause(6000);
            JobRunLogger.info("演示异步工作完成");
        });
        JobRunLogger.info("主方法返回，继续等待异步工作完成");
    }
}
