package com.faber.api.base.admin.vo.ret;

/** 业务执行记录所属的流程轮次与节点快照，不包含定义或日志正文。 */
public record JobFlowLogOrigin(Integer jobId, Integer flowLogId, String nodeId, String nodeName, String flowName) {}
