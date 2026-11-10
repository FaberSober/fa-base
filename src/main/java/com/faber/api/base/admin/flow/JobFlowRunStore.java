package com.faber.api.base.admin.flow;

import com.faber.api.base.admin.vo.ret.JobFlowHistoryStats;
import com.faber.core.vo.msg.TableRet;
import com.faber.api.base.admin.vo.req.JobFlowHistoryQuery;
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
    public JobFlowRunView begin(Integer jobId) { return beginRun(jobId, false); }

    @Transactional(rollbackFor = Exception.class)
    public JobFlowRunView beginScheduled(Integer jobId) { return beginRun(jobId, true); }

    private JobFlowRunView beginRun(Integer jobId, boolean scheduled) {
        Job job = jobs.selectOne(new QueryWrapper<Job>().eq("id", jobId).last("FOR UPDATE"));
        if (job == null) { if (scheduled) return null; throw new NoDataException(); }
        if (!Integer.valueOf(1).equals(job.getJobType())) throw new BuzzException("请选择流程任务");
        if (scheduled && !Boolean.TRUE.equals(job.getStatus())) return null;
        if (logs.selectCount(new QueryWrapper<JobLog>().eq("job_id", jobId).eq("job_type", 1).in("status", 1, 3, 8)) > 0) {
            if (scheduled) return null;
            throw new BuzzException("该流程仍有执行中或未确认的运行，不能重复启动");
        }
        Set<String> registered = RegisteredJobService.registeredPaths();
        JobFlowDefinitionValidator.validate(job.getFlowDefinition(), registered);
        JobDefinitionVersion version = definitionVersions.ensureCurrent(job);
        JobLog parent = new JobLog();
        parent.setJobId(jobId); parent.setJobType(1); parent.setTriggerSource(scheduled ? 2 : 1); parent.setJobName(job.getJobName());
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

    public JobFlowRunView view(Integer logId) { return view(logId, null); }
    public JobFlowRunView view(Integer logId, Integer jobId) {
        JobLog parent = logs.selectById(logId);
        if (parent == null || !Integer.valueOf(1).equals(parent.getJobType()) || (jobId != null && !jobId.equals(parent.getJobId()))) throw new NoDataException();
        return new JobFlowRunView(parent, nodeRuns(logId), definitionVersions.definition(parent));
    }

    /** 单次有界游标扫描，不加载历史正文或定义。 */
    public List<JobLog> activeRunPage(int afterId) {
        return logs.selectPage(new Page<JobLog>(1, 100, false), new QueryWrapper<JobLog>()
                .select("id", "job_id").eq("job_type", 1).in("status", 1, 3)
                .gt("id", afterId).orderByAsc("id")).getRecords();
    }

    public TableRet<JobLog> historyPage(Integer jobId, JobFlowHistoryQuery request) {
        QueryWrapper<JobLog> query = historyQuery(jobId, request);
        query.select("id", "job_id", "job_type", "trigger_source", "job_name", "status", "begin_time", "end_time", "duration_ms", "definition_version_id").orderByDesc("id");
        return new TableRet<>(logs.selectPage(new Page<>(request.getCurrent(), request.getPageSize()), query));
    }

    public JobFlowHistoryStats historyStats(Integer jobId, JobFlowHistoryQuery request) {
        QueryWrapper<JobLog> query = historyQuery(jobId, request);
        String sample = "status IN (2,9) AND tracking_version=1 AND duration_ms IS NOT NULL AND duration_ms>=0";
        query.select("COUNT(*) AS total", "SUM(CASE WHEN status=1 THEN 1 ELSE 0 END) AS doing",
                "SUM(CASE WHEN status=2 THEN 1 ELSE 0 END) AS success", "SUM(CASE WHEN status=9 THEN 1 ELSE 0 END) AS failed",
                "SUM(CASE WHEN status=8 THEN 1 ELSE 0 END) AS unconfirmed",
                "SUM(CASE WHEN status=3 THEN 1 ELSE 0 END) AS stopping", "SUM(CASE WHEN status=7 THEN 1 ELSE 0 END) AS stopped",
                "SUM(CASE WHEN " + sample + " THEN 1 ELSE 0 END) AS duration_samples",
                "AVG(CASE WHEN " + sample + " THEN duration_ms ELSE NULL END) AS avg_duration_ms");
        Map<String, Object> values = new HashMap<>();
        logs.selectMaps(query).get(0).forEach((key, value) -> values.put(key.toLowerCase(Locale.ROOT), value));
        Object average = values.get("avg_duration_ms");
        Long avg = average == null ? null : new java.math.BigDecimal(average.toString()).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
        return new JobFlowHistoryStats(statCount(values, "total"), statCount(values, "doing"),
                statCount(values, "success"), statCount(values, "failed"), statCount(values, "unconfirmed"), statCount(values, "duration_samples"), avg, statCount(values, "stopping"), statCount(values, "stopped"));
    }
    private static Long statCount(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? 0L : ((Number) value).longValue();
    }
    private QueryWrapper<JobLog> historyQuery(Integer jobId, JobFlowHistoryQuery request) {
        if (jobId == null || jobId < 1 || request == null || request.getCurrent() < 1 || !Set.of(20, 50, 100).contains(request.getPageSize()))
            throw new BuzzException("分页参数无效，每页条数仅支持20/50/100");
        var filters = request.getQuery();
        QueryWrapper<JobLog> query = new QueryWrapper<JobLog>().eq("job_id", jobId).eq("job_type", 1);
        if (filters != null) {
            if (filters.getLogId() != null) {
                if (filters.getLogId() < 1) throw new BuzzException("运行ID须为正整数");
                query.eq("id", filters.getLogId());
            }
            if (filters.getStatus() != null) {
                if (!Set.of(1, 2, 3, 7, 8, 9).contains(filters.getStatus())) throw new BuzzException("执行状态无效");
                query.eq("status", filters.getStatus());
            }
            if (filters.getTriggerSource() != null) {
                if (!Set.of(0, 1, 2).contains(filters.getTriggerSource())) throw new BuzzException("触发来源无效");
                query.eq("trigger_source", filters.getTriggerSource());
            }
            java.sql.Timestamp from = historyTime(filters.getBeginTimeFrom()), to = historyTime(filters.getBeginTimeTo());
            if (from != null && to != null && from.after(to)) throw new BuzzException("开始时间范围无效");
            if (from != null) query.ge("begin_time", from);
            if (to != null) query.le("begin_time", to);
        }
        return query;
    }
    private static java.sql.Timestamp historyTime(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            var format = java.time.format.DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(java.time.format.ResolverStyle.STRICT);
            return java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(value, format));
        } catch (java.time.DateTimeException error) { throw new BuzzException("时间格式须为yyyy-MM-dd HH:mm:ss且日期有效"); }
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
        if (parent == null) return null;
        if (parent.getStatus() == JobLogStatusEnum.STOPPING) {
            List<JobFlowNodeRun> stopping = nodeRuns(logId);
            if (stopping.stream().noneMatch(run -> run.getStatus() == 30)) finishParent(parent, stopping);
            return null;
        }
        if (parent.getStatus() != JobLogStatusEnum.DOING) return null;
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

    /** 仅用已提交的业务终态补齐遗漏回调，不重跑、不猜测仍在执行的业务。 */
    @Transactional(rollbackFor = Exception.class)
    public JobFlowRunView reconcile(Integer logId, Integer jobId) {
        JobLog parent = logs.selectOne(new QueryWrapper<JobLog>().eq("id", logId).last("FOR UPDATE"));
        if (parent == null || !Integer.valueOf(1).equals(parent.getJobType()) || !Objects.equals(jobId, parent.getJobId())) throw new NoDataException();
        if (parent.getStatus() != JobLogStatusEnum.DOING && parent.getStatus() != JobLogStatusEnum.STOPPING) return view(logId, jobId);
        for (JobFlowNodeRun run : nodeRuns(logId)) {
            if (!Set.of(20, 30).contains(run.getStatus()) || run.getJobLogId() == null) continue;
            JobLog business = logs.selectOne(new QueryWrapper<JobLog>().eq("id", run.getJobLogId()).last("FOR UPDATE"));
            if (business == null || business.getStatus() == null) continue;
            Integer state = switch (business.getStatus()) {
                case DONE -> 40;
                case ERROR -> 50;
                case UNCONFIRMED -> 80;
                case STOPPED -> 70;
                default -> null;
            };
            if (state == null) continue;
            nodes.update(null, new UpdateWrapper<JobFlowNodeRun>().eq("id", run.getId()).in("status", 20, 30)
                    .set("status", state).set("end_time", business.getEndTime()).set("reason", business.getErrMsg()).set("upd_time", new Date()));
        }
        return view(logId, jobId);
    }

    /** 取消尚未调用的节点；运行中的工作继续真实收尾。 */
    @Transactional(rollbackFor = Exception.class)
    public JobFlowRunView stop(Integer logId, Integer jobId) {
        JobLog parent = logs.selectOne(new QueryWrapper<JobLog>().eq("id", logId).last("FOR UPDATE"));
        if (parent == null || !Integer.valueOf(1).equals(parent.getJobType()) || !Objects.equals(jobId, parent.getJobId())) throw new NoDataException();
        if (parent.getStatus() != JobLogStatusEnum.DOING && parent.getStatus() != JobLogStatusEnum.STOPPING) return view(logId, jobId);
        logs.update(null, new UpdateWrapper<JobLog>().eq("id", logId).set("status", 3).set("err_msg", "已请求停止，等待已启动任务收尾"));
        parent.setStatus(JobLogStatusEnum.STOPPING);
        jobs.update(null, new UpdateWrapper<Job>().eq("id", jobId).eq("last_job_log_id", logId).set("last_status", 3));
        for (JobFlowNodeRun run : nodeRuns(logId)) if (Set.of(0, 10, 20).contains(run.getStatus())) {
            Date now = new Date();
            if (run.getStatus() == 20 && run.getJobLogId() != null) {
                JobLog business = logs.selectOne(new QueryWrapper<JobLog>().eq("id", run.getJobLogId()).last("FOR UPDATE"));
                if (business.getStatus() != JobLogStatusEnum.DOING) {
                    run.setStatus(business.getStatus() == JobLogStatusEnum.DONE ? 40 : business.getStatus() == JobLogStatusEnum.ERROR ? 50 : business.getStatus() == JobLogStatusEnum.STOPPED ? 70 : 80);
                    run.setEndTime(business.getEndTime()); run.setReason(business.getErrMsg()); run.setUpdTime(now); nodes.updateById(run);
                    continue;
                }
                business.setStatus(JobLogStatusEnum.STOPPED); business.setEndTime(now); business.setTrackingVersion(1);
                business.setDurationMs(Math.max(0, now.getTime() - business.getBeginTime().getTime()));
                business.setDuration(business.getDurationMs() / 1000); business.setErrMsg("流程停止，业务尚未调用");
                if (!logBiz.finishExecution(business)) throw new IllegalStateException("取消排队节点日志失败");
            }
            run.setStatus(70); run.setReason("人工停止，业务未启动"); run.setEndTime(now); run.setUpdTime(now); nodes.updateById(run);
        }
        List<JobFlowNodeRun> runs = nodeRuns(logId);
        if (runs.stream().noneMatch(run -> run.getStatus() == 30)) finishParent(parent, runs);
        return view(logId, jobId);
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
        if (parent == null) return;
        if (parent.getStatus() == JobLogStatusEnum.STOPPING) {
            List<JobFlowNodeRun> stopping = nodeRuns(logId);
            if (stopping.stream().noneMatch(run -> run.getStatus() == 30)) finishParent(parent, stopping);
            return;
        }
        if (parent.getStatus() != JobLogStatusEnum.DOING) return;
        List<JobFlowNodeRun> runs = nodeRuns(logId);
        for (JobFlowNodeRun run : runs) if (run.getStatus() == 0 || run.getStatus() == 10) {
            run.setStatus(60); run.setReason(reason); run.setUpdTime(new Date()); nodes.updateById(run);
        }
        // 执行器拒绝后停止新领取；已接受的并行工作仍需等待真实终态再汇总。
        if (runs.stream().noneMatch(run -> run.getStatus() == 20 || run.getStatus() == 30)) finishParent(parent, runs);
    }
    private void finishParent(JobLog parent, List<JobFlowNodeRun> runs) {
        boolean stopped = parent.getStatus() == JobLogStatusEnum.STOPPING || runs.stream().anyMatch(run -> run.getStatus() == 70);
        boolean unknown = runs.stream().anyMatch(run -> run.getStatus() == 80);
        boolean failed = runs.stream().anyMatch(run -> run.getStatus() == 50 || run.getStatus() == 60);
        parent.setStatus(unknown ? JobLogStatusEnum.UNCONFIRMED : failed ? JobLogStatusEnum.ERROR : stopped ? JobLogStatusEnum.STOPPED : JobLogStatusEnum.DONE);
        parent.setTrackingVersion(unknown ? 2 : 1); parent.setEndTime(unknown ? null : new Date());
        parent.setDurationMs(unknown ? null : Math.max(0, parent.getEndTime().getTime() - parent.getBeginTime().getTime()));
        parent.setDuration(unknown ? 0L : parent.getDurationMs() / 1000);
        parent.setErrMsg(unknown ? "节点结果未确认，后续未放行" : failed ? "部分节点失败或被阻断，请查看节点记录" : stopped ? "人工停止本轮流程" : null);
        logBiz.finishExecution(parent);
    }
    /** 重启不重跑任何可能产生副作用的节点；未确认结果继续占用该流程运行互斥。 */
    @Transactional(rollbackFor = Exception.class)
    public void recoverInterrupted() {
        for (JobLog candidate : logs.selectList(new QueryWrapper<JobLog>().eq("job_type", 1).in("status", 1, 3))) {
            JobLog parent = logs.selectOne(new QueryWrapper<JobLog>().eq("id", candidate.getId()).last("FOR UPDATE"));
            if (parent == null || (parent.getStatus() != JobLogStatusEnum.DOING && parent.getStatus() != JobLogStatusEnum.STOPPING)) continue;
            boolean stopping = parent.getStatus() == JobLogStatusEnum.STOPPING;
            for (JobFlowNodeRun run : nodeRuns(parent.getId())) {
                if (run.getStatus() == 20 || run.getStatus() == 30) {
                    JobLog business = run.getJobLogId() == null ? null : logs.selectOne(new QueryWrapper<JobLog>()
                            .eq("id", run.getJobLogId()).last("FOR UPDATE"));
                    Integer known = business == null || business.getStatus() == null ? null : switch (business.getStatus()) {
                        case DONE -> 40;
                        case ERROR -> 50;
                        case STOPPED -> 70;
                        case UNCONFIRMED -> 80;
                        default -> null;
                    };
                    boolean incomplete = known != null && known != 80 && !Integer.valueOf(1).equals(business.getTrackingVersion());
                    if (known != null) {
                        run.setStatus(incomplete ? 80 : known);
                        run.setEndTime(incomplete || known == 80 ? null : business.getEndTime());
                        run.setReason(incomplete ? "服务重启，业务记录不是完整跟踪口径，无法确认实际完成" : business.getErrMsg());
                    } else {
                        if (business != null && business.getStatus() == JobLogStatusEnum.DOING) {
                            business.setStatus(JobLogStatusEnum.UNCONFIRMED); business.setTrackingVersion(2); business.setDuration(0L);
                            business.setEndTime(null); business.setDurationMs(null);
                            business.setErrMsg("服务重启，无法确认节点业务结果"); logBiz.finishExecution(business);
                        }
                        run.setStatus(80); run.setEndTime(null); run.setReason("服务重启，无法确认节点是否完成");
                    }
                } else if (run.getStatus() == 0 || run.getStatus() == 10) {
                    run.setStatus(stopping ? 70 : 60); run.setReason("服务重启，后续节点不再启动");
                }
                run.setUpdTime(new Date()); nodes.updateById(run);
            }
            finishParent(parent, nodeRuns(parent.getId()));
        }
    }
}
