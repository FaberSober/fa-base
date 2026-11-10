package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.api.base.admin.mapper.JobMapper;
import com.faber.api.base.admin.mapper.JobFlowNodeRunMapper;
import com.faber.api.base.admin.entity.JobFlowNodeRun;
import com.faber.api.base.admin.mapper.JobLogMapper;
import com.faber.api.base.admin.vo.ret.JobLogReadVo;
import com.faber.api.base.admin.vo.ret.JobFlowLogOrigin;
import com.faber.config.quartz.logging.JobLogFileService;
import com.faber.core.constant.FaSetting;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.NoDataException;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * BASE-系统定时任务-执行日志
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-07 16:03:51
 */
@Service
@Slf4j
public class JobLogBiz extends BaseBiz<JobLogMapper, JobLog> {
    @Resource private JobMapper jobMapper;
    @Resource private JobFlowNodeRunMapper flowNodeRuns;
    @Resource private JobLogFileService logFiles;
    @Resource private FaSetting faSetting;

    /** 开始记录和主表计数属于同一事务，避免只有日志或只有计数。 */
    @Transactional(rollbackFor = Exception.class)
    public void beginExecution(JobLog record) {
        if (!save(record)) throw new IllegalStateException("登记任务执行失败");
        if (record.getJobId() == null) return;
        if (jobMapper.incrementExecutionCount(record) != 1) {
            throw new IllegalStateException("任务不存在或已删除");
        }
        jobMapper.publishStart(record);
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordDispatch(JobLog record) {
        if (baseMapper.publishDispatch(record) == 1 && record.getJobId() != null) jobMapper.publishDispatch(record);
    }

    /** 条件终态更新是唯一累计入口；重复回调、重试不重复计数。 */
    @Transactional(rollbackFor = Exception.class)
    public boolean finishExecution(JobLog record) {
        if (record.getStatus() == null || (record.getStatus() == JobLogStatusEnum.DOING || record.getStatus() == JobLogStatusEnum.STOPPING)) {
            throw new IllegalArgumentException("任务尚未结束");
        }
        if (baseMapper.finishExecution(record) != 1) return false;
        if (record.getJobId() != null) {
            jobMapper.incrementFinishedStats(record);
            jobMapper.publishFinish(record);
        }
        return true;
    }

    /** 开关按每次执行快照；日志设施故障不影响任务本身。 */
    public JobLogFileService.ExecutionLog openExecutionLog(JobLog record) {
        try {
            Job job = record.getJobId() == null ? null : jobMapper.selectById(record.getJobId());
            return openExecutionLog(record, job != null && Boolean.TRUE.equals(job.getDetailLogEnabled()));
        } catch (Exception e) {
            log.error("读取任务详细日志配置失败，执行记录：{}", record.getId(), e);
            return null;
        }
    }

    public JobLogFileService.ExecutionLog openExecutionLog(JobLog record, boolean detailLogEnabled) {
        JobLogFileService.ExecutionLog execution = null;
        try {
            if (!detailLogEnabled) return null;
            execution = logFiles.open(record);
            record.setLogPath(execution.getRelativePath());
            if (!updateById(record)) throw new IllegalStateException("登记任务日志路径失败");
            return execution;
        } catch (Exception e) {
            if (execution != null) execution.close();
            record.setLogPath(null);
            log.error("初始化任务详细日志失败，执行记录：{}", record.getId(), e);
            return null;
        }
    }

    /** 归属只由服务端节点关联确定；同类任务的不同节点不得混淆。 */
    public JobFlowLogOrigin flowOrigin(Integer id) {
        requireRecord(id);
        JobFlowNodeRun node = flowNodeRuns.selectOne(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<JobFlowNodeRun>()
                .eq("job_log_id", id));
        if (node == null) return null;
        JobLog parent = getById(node.getFlowLogId());
        if (parent == null || !Integer.valueOf(1).equals(parent.getJobType()) || parent.getJobId() == null)
            throw new NoDataException();
        return new JobFlowLogOrigin(parent.getJobId(), parent.getId(), node.getNodeId(), node.getNodeName(), parent.getJobName());
    }

    public JobLogReadVo readLog(Integer id, long offset, int maxBytes) {
        // 必须先读取状态，再读取文件，避免 EOF 与终态之间的竞态漏掉最后日志。
        JobLog record = requireRecord(id);
        try {
            JobLogReadVo result = logFiles.read(record.getLogPath(), offset, maxBytes,
                    (record.getStatus() != JobLogStatusEnum.DOING && record.getStatus() != JobLogStatusEnum.STOPPING));
            result.setStatus(record.getStatus());
            return result;
        } catch (IOException e) {
            throw new BuzzException("读取任务日志失败，请稍后重试");
        }
    }

    public void downloadLog(Integer id, HttpServletResponse response) throws IOException {
        JobLog record = requireRecord(id);
        if (record.getLogPath() == null) throw new BuzzException("本次执行未记录详细日志");
        Path path = logFiles.resolve(record.getLogPath());
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new BuzzException("日志已清理或不可用");
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"job-" + id + ".log\"");
        response.setHeader("Fa-Filename", "job-" + id + ".log");
        Files.copy(path, response.getOutputStream());
    }

    public int cleanupExpiredFiles() throws IOException {
        int days = faSetting.getJobLog().getRetentionDays();
        if (days <= 0) return 0;
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        return logFiles.cleanup(cutoff, (id, relativePath) -> {
            JobLog record = getById(id);
            if (record == null || !relativePath.equals(record.getLogPath())) return true;
            Date retentionEnd = record.getEndTime();
            // 未确认记录没有真实业务结束时间；文件服务还会校验文件最后写入时间。
            if (record.getStatus() == JobLogStatusEnum.UNCONFIRMED && retentionEnd == null) {
                retentionEnd = record.getDispatchEndTime();
            }
            return (record.getStatus() != JobLogStatusEnum.DOING && record.getStatus() != JobLogStatusEnum.STOPPING) && retentionEnd != null
                    && retentionEnd.toInstant().isBefore(cutoff);
        });
    }

    private JobLog requireRecord(Serializable id) {
        JobLog record = getById(id);
        if (record == null) throw new NoDataException();
        return record;
    }

    private void checkRemovable(Collection<JobLog> records) {
        if (records.stream().anyMatch(record -> Integer.valueOf(1).equals(record.getJobType())
                || flowNodeRuns.selectCount(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<JobFlowNodeRun>()
                        .eq(JobFlowNodeRun::getJobLogId, record.getId())) > 0)) {
            throw new BuzzException("流程关联日志暂不支持删除");
        }
        if (records.stream().anyMatch(record -> (record.getStatus() == JobLogStatusEnum.DOING || record.getStatus() == JobLogStatusEnum.STOPPING))) {
            throw new BuzzException("任务执行中，请结束后再删除执行记录");
        }
    }

    @Override
    public boolean removeById(Serializable id) {
        JobLog record = getById(id);
        if (record == null) return false;
        checkRemovable(List.of(record));
        boolean removed = super.removeById(id);
        if (removed) logFiles.delete(record.getLogPath());
        return removed;
    }

    @Override
    public void removeBatchByIds(List<Serializable> ids) {
        if (ids == null || ids.isEmpty()) return;
        List<JobLog> records = listByIds(ids);
        checkRemovable(records);
        super.removeBatchByIds(ids);
        records.forEach(record -> logFiles.delete(record.getLogPath()));
    }

    @Override
    public void removePerById(Serializable id) { removeById(id); }

    @Override
    public void removePerByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return;
        removeBatchByIds(ids.stream().map(id -> (Serializable) id).toList());
    }

    @Override
    public void removePerBatchByIds(List<Serializable> ids) { removeBatchByIds(ids); }

    @Override
    public void removeByQuery(QueryParams query) {
        if (count(getQueryWrapper(query)) > 1000) throw new BuzzException("删除数据超过1000条，请使用批量删除");
        List<JobLog> records = list(parseQuery(query));
        removeBatchByIds(records.stream().map(record -> (Serializable) record.getId()).toList());
    }
}
