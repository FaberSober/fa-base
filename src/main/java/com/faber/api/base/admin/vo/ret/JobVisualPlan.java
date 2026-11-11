package com.faber.api.base.admin.vo.ret;

import java.util.List;

public record JobVisualPlan(List<Item> items) {
    public record Item(Integer jobId, String jobName, Integer jobType, String cron, boolean paused,
                       int start, int end, Long durationMs, int count) {}
}
