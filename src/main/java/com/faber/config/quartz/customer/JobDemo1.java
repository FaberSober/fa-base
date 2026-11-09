package com.faber.config.quartz.customer;

import cn.hutool.core.util.RandomUtil;
import com.faber.config.quartz.BaseJob;
import com.faber.config.quartz.logging.JobRunLogger;
import com.faber.core.annotation.FaJob;
import lombok.extern.slf4j.Slf4j;

/**
 * 定时任务DEMO
 * @author xu.pengfei
 * @date 2022/11/28 13:41
 */
@Slf4j
@FaJob(value = "定时任务DEMO", group = "流程演示", description = "随机成功或失败的三批次演示任务", tags = {"演示"})
public class JobDemo1 extends BaseJob {

    @Override
    protected void run() {
        JobRunLogger.info("开始示例任务，共 3 个批次");
        try {
            for (int batch = 1; batch <= 3; batch++) {
                Thread.sleep(1000);
                JobRunLogger.info("已处理 {}/3 个批次", batch);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("示例任务已中断", e);
        }
        if (RandomUtil.randomInt(100) > 50) {
            throw new RuntimeException("定时任务DEMO测试抛出异常信息");
        }
    }

}
