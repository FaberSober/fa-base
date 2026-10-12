package com.faber.api.base.admin.jobs;

import com.faber.config.quartz.BaseJob;
import com.faber.core.annotation.FaJob;
import lombok.extern.slf4j.Slf4j;

/**
 * 已停用的旧版按数量清理任务。
 *
 * 保留该类以兼容已配置的 Quartz 任务；日志生命周期由归档和独立清理任务接管。
 * @author xu.pengfei
 * @date 2025/05/30 15:35
 */
@Slf4j
@Deprecated
@FaJob("旧版日志数量清理（已停用）")
public class JobLogApiDeleteOverSize extends BaseJob {

    @Override
    protected void run() {
        log.warn("旧版日志数量清理任务已停用，请改用日志归档生命周期配置");
    }

}
