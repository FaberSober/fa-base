package com.faber.config.quartz.customer;

import com.faber.config.quartz.BaseJob;
import com.faber.core.annotation.FaJob;

/** 固定失败且无业务副作用，用于验证后代阻断。 */
@FaJob("流程演示：固定失败")
public class JobFlowDemoFailure extends BaseJob {
    @Override protected void run() {
        throw new IllegalStateException("流程演示预期失败，用于验证依赖阻断");
    }
}
