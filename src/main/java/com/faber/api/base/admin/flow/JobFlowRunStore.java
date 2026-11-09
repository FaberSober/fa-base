package com.faber.api.base.admin.flow;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.faber.api.base.admin.biz.JobLogBiz;
import com.faber.api.base.admin.entity.*;
import com.faber.api.base.admin.mapper.*;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.NoDataException;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** 锁定父运行记录后推进，一次仅领取一个节点。所有业务执行必须在事务返回后发生。 */
@Service
public class JobFlowRunStore {
    @Resource private JobMapper jobs;
    @Resource private JobLogMapper logs;
    @Resource private JobFlowNodeRunMapper nodes;
    @Resource private JobLogBiz logBiz;
    @Resource private JobDefinitionVersions definitionVersions;
    public record Launch(JobFlowNodeRun node, JobLog log, boolean detailLogEnabled) {}

    @Transactional(rollbackFor = Exception.class)
    public JobFlowRunView begin(Integer jobId) {
        Job job = jobs.selectOne(new QueryWrapper<Job>().eq("id", jobId).last("FOR UPDATE"));
        if (job == null) throw new NoDataException();
        if (!Integer.valueOf(1).equals(job.getJobType())) throw new BuzzException("请选择流程任务");
        if (logs.selectCount(new QueryWrapper<JobLog>().eq("job_id", jobId).eq("job_type", 1).in("status", 1, 8)) > 0)
            throw new BuzzException("该流程仍有执行中或未确认的运行，不能重复启动");
        Set<String> registered = RegisteredJobService.registeredPaths();
        JobFlowDefinitionValidator.validate(job.getFlowDefinition(), registered);
        JobDefinitionVersion version = definitionVersions.ensureCurrent(job);
        JobLog parent = new JobLog();
        parent.setJobId(jobId); parent.setJobType(1); parent.setTriggerSource(1); parent.setJobName(job.getJobName());
        parent.setDefinitionVersionId(version.getId()); parent.setBeginTime(new Date()); parent.setStatus(JobLogStatusEnum.DOING); parent.setTrackingVersion(1); parent.setDuration(0L);
        JsonNode definition = definitionVersions.definition(parent);
        logBiz.beginExecution(parent);
        for (JsonNode node : definition.path("nodes")) {
            JobFlowNodeRun run = new JobFlowNodeRun();
            run.setFlowLogId(parent.getId()); run.setNodeId(node.path("id").asText()); run.setNodeName(node.path("name").asText()); run.setClazzPath(node.path("clazzPath").asText());
            run.setAttempt(1); run.setStatus(0); run.setCrtTime(new Date()); run.setUpdTime(run.getCrtTime());
            if (nodes.insert(run) != 1) throw new IllegalStateException("创建节点运行记录失败");
        }
        parent.setDispatchEndTime(new Date()); parent.setDispatchDurationMs(Math.max(0, parent.getDispatchEndTime().getTime() - parent.getBeginTime().getTime()));
        logBiz.recordDispatch(parent);
        return view(parent.getId());
    }

    public JobFlowRunView view(Integer logId) {
        JobLog parent = logs.selectById(logId);
        if (parent == null || !Integer.valueOf(1).equals(parent.getJobType())) throw new NoDataException();
        return new JobFlowRunView(parent, nodeRuns(logId), definitionVersions.definition(parent));
    }
    private List<JobFlowNodeRun> nodeRuns(Integer logId) {
        return nodes.selectList(new QueryWrapper<JobFlowNodeRun>().eq("flow_log_id", logId).orderByAsc("id"));
    }
    public List<JobLog> history(Integer jobId) {
        return logs.selectPage(new Page<>(1, 20), new QueryWrapper<JobLog>().eq("job_id", jobId).eq("job_type", 1).orderByDesc("id")).getRecords();
    }

    @Transactional(rollbackFor = Exception.class)
    public Launch claim(Integer logId) {
        JobLog parent = logs.selectOne(new QueryWrapper<JobLog>().eq("id", logId).last("FOR UPDATE"));
        if (parent == null || parent.getStatus() != JobLogStatusEnum.DOING) return null;
        List<JobFlowNodeRun> runs = nodeRuns(logId);
        JsonNode definition = definitionVersions.definition(parent);
        var decision = JobFlowPlanner.next(definition, runs);
        for (JobFlowNodeRun run : runs) if (decision.blocked().contains(run.getNodeId())) {
            run.setStatus(60); run.setReason(runs.stream().anyMatch(item -> item.getStatus() == 80) ? "存在未确认工作，暂停启动新节点" : "前驱失败或阻断，未启动"); run.setUpdTime(new Date()); nodes.updateById(run);
        }
        if (decision.next() == null) {
            if (runs.stream().noneMatch(run -> Set.of(0, 10, 20, 30).contains(run.getStatus()))) finishParent(parent, runs);
            return null;
        }
        JobFlowNodeRun run = runs.stream().filter(item -> item.getNodeId().equals(decision.next())).findFirst().orElseThrow();
        JobLog business = new JobLog();
        business.setJobType(0); business.setTriggerSource(3); business.setJobName(run.getNodeName()); business.setClazzPath(run.getClazzPath());
        business.setBeginTime(new Date()); business.setDuration(0L); business.setTrackingVersion(1); business.setStatus(JobLogStatusEnum.DOING);
        logBiz.beginExecution(business);
        run.setJobLogId(business.getId()); run.setBeginTime(business.getBeginTime()); run.setStatus(20); run.setUpdTime(new Date());
        if (nodes.updateById(run) != 1) throw new IllegalStateException("登记节点启动失败");
        return new Launch(run, business, definition.path("detailLogEnabled").asBoolean(false));
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeNode(JobLog business) {
        JobFlowNodeRun run = nodes.selectOne(new QueryWrapper<JobFlowNodeRun>().eq("job_log_id", business.getId()));
        if (run == null) return false;
        // 与领取、整体汇总使用相同的父锁顺序。
        logs.selectOne(new QueryWrapper<JobLog>().eq("id", run.getFlowLogId()).last("FOR UPDATE"));
        int state = business.getStatus() == JobLogStatusEnum.DONE ? 40 : business.getStatus() == JobLogStatusEnum.ERROR ? 50 : 80;
        return nodes.update(null, new UpdateWrapper<JobFlowNodeRun>().eq("id", run.getId()).in("status", 20, 30)
                .set("status", state).set("end_time", business.getEndTime()).set("reason", business.getErrMsg()).set("upd_time", new Date())) == 1;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markRunning(Integer id) {
        JobFlowNodeRun run = nodes.selectById(id);
        if (run == null) return false;
        JobLog parent = logs.selectOne(new QueryWrapper<JobLog>().eq("id", run.getFlowLogId()).last("FOR UPDATE"));
        if (parent == null || parent.getStatus() != JobLogStatusEnum.DOING) return false;
        return nodes.update(null, new UpdateWrapper<JobFlowNodeRun>().eq("id", id).eq("status", 20).set("status", 30).set("upd_time", new Date())) == 1;
    }
    @Transactional(rollbackFor = Exception.class)
    public void failSubmission(Integer logId, String reason) {
        JobLog parent = logs.selectOne(new QueryWrapper<JobLog>().eq("id", logId).last("FOR UPDATE"));
        if (parent == null || parent.getStatus() != JobLogStatusEnum.DOING) return;
        List<JobFlowNodeRun> runs = nodeRuns(logId);
        for (JobFlowNodeRun run : runs) if (run.getStatus() == 0 || run.getStatus() == 10) {
            run.setStatus(60); run.setReason(reason); run.setUpdTime(new Date()); nodes.updateById(run);
        }
        // 执行器拒绝后停止新领取；已接受的并行工作仍需等待真实终态再汇总。
        if (runs.stream().noneMatch(run -> run.getStatus() == 20 || run.getStatus() == 30)) finishParent(parent, runs);
    }
    private void finishParent(JobLog parent, List<JobFlowNodeRun> runs) {
        boolean unknown = runs.stream().anyMatch(run -> run.getStatus() == 80);
        boolean failed = runs.stream().anyMatch(run -> run.getStatus() == 50 || run.getStatus() == 60);
        parent.setStatus(unknown ? JobLogStatusEnum.UNCONFIRMED : failed ? JobLogStatusEnum.ERROR : JobLogStatusEnum.DONE);
        parent.setTrackingVersion(unknown ? 2 : 1); parent.setEndTime(unknown ? null : new Date());
        parent.setDurationMs(unknown ? null : Math.max(0, parent.getEndTime().getTime() - parent.getBeginTime().getTime()));
        parent.setDuration(unknown ? 0L : parent.getDurationMs() / 1000);
        parent.setErrMsg(unknown ? "节点结果未确认，后续未放行" : failed ? "部分节点失败或被阻断，请查看节点记录" : null);
        logBiz.finishExecution(parent);
    }
    /** 重启不重跑任何可能产生副作用的节点；未确认结果继续占用该流程运行互斥。 */
    @Transactional(rollbackFor = Exception.class)
    public void recoverInterrupted() {
        for (JobLog parent : logs.selectList(new QueryWrapper<JobLog>().eq("job_type", 1).eq("status", 1))) {
            logs.selectOne(new QueryWrapper<JobLog>().eq("id", parent.getId()).last("FOR UPDATE"));
            for (JobFlowNodeRun run : nodeRuns(parent.getId())) {
                if (run.getStatus() == 20 || run.getStatus() == 30) {
                    if (run.getJobLogId() != null) {
                        JobLog business = logs.selectById(run.getJobLogId());
                        if (business != null && business.getStatus() == JobLogStatusEnum.DOING) {
                            business.setStatus(JobLogStatusEnum.UNCONFIRMED); business.setTrackingVersion(2); business.setDuration(0L);
                            business.setErrMsg("服务重启，无法确认节点业务结果"); logBiz.finishExecution(business);
                        }
                    }
                    run.setStatus(80); run.setReason("服务重启，无法确认节点是否完成");
                } else if (run.getStatus() == 0 || run.getStatus() == 10) { run.setStatus(60); run.setReason("服务重启，后续节点不再启动"); }
                run.setUpdTime(new Date()); nodes.updateById(run);
            }
            parent.setStatus(JobLogStatusEnum.UNCONFIRMED); parent.setTrackingVersion(2); parent.setDuration(0L);
            parent.setErrMsg("服务重启，整轮结果未确认，禁止自动重跑"); logBiz.finishExecution(parent);
        }
    }
}
