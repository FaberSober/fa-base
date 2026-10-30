package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.api.base.admin.mapper.JobMapper;
import com.faber.api.base.admin.mapper.JobLogMapper;
import com.faber.api.base.admin.vo.ret.JobLogReadVo;
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

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
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
    @Resource private JobLogFileService logFiles;
    @Resource private FaSetting faSetting;

    /** 开关按每次执行快照；日志设施故障不影响任务本身。 */
    public JobLogFileService.ExecutionLog openExecutionLog(JobLog record) {
        JobLogFileService.ExecutionLog execution = null;
        try {
            Job job = jobMapper.selectById(record.getJobId());
            if (job == null || !Boolean.TRUE.equals(job.getDetailLogEnabled())) return null;
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

    public JobLogReadVo readLog(Integer id, long offset, int maxBytes) {
        // 必须先读取状态，再读取文件，避免 EOF 与终态之间的竞态漏掉最后日志。
        JobLog record = requireRecord(id);
        try {
            JobLogReadVo result = logFiles.read(record.getLogPath(), offset, maxBytes,
                    record.getStatus() != JobLogStatusEnum.DOING);
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
            return record.getStatus() != JobLogStatusEnum.DOING && record.getEndTime() != null
                    && record.getEndTime().toInstant().isBefore(cutoff);
        });
    }

    private JobLog requireRecord(Serializable id) {
        JobLog record = getById(id);
        if (record == null) throw new NoDataException();
        return record;
    }

    private void checkRemovable(Collection<JobLog> records) {
        if (records.stream().anyMatch(record -> record.getStatus() == JobLogStatusEnum.DOING)) {
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
