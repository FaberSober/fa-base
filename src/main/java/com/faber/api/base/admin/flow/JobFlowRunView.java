package com.faber.api.base.admin.flow;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.entity.JobFlowNodeRun;
import java.util.List;
public record JobFlowRunView(JobLog log, List<JobFlowNodeRun> nodes, com.fasterxml.jackson.databind.JsonNode definition) {}
