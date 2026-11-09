package com.faber.api.base.admin.vo.ret;

/** 当前查询范围内保留的流程整体运行统计，不包含节点业务日志。 */
public record JobFlowHistoryStats(Long total, Long doing, Long success, Long failed, Long unconfirmed,
                                  Long durationSamples, Long avgDurationMs) {}
