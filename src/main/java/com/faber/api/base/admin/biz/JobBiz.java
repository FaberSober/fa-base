package com.faber.api.base.admin.biz;

import cn.hutool.core.date.DateUtil;
import com.faber.api.base.admin.flow.RegisteredJobService;
import com.faber.api.base.admin.vo.ret.RegisteredJobVo;
import cn.hutool.core.util.ObjUtil;
import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.flow.JobFlowDefinitionValidator;
import com.faber.api.base.admin.flow.JobFlowPreflight;
import com.faber.api.base.admin.flow.JobDefinitionVersions;
import java.util.Collection;
import java.util.Set;
import com.faber.api.base.admin.mapper.JobMapper;
import com.faber.api.base.admin.mapper.JobLogMapper;
import com.faber.api.base.admin.entity.JobLog;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.core.vo.query.QueryParams;
import java.io.Serializable;
import com.faber.config.quartz.JobTask;
import com.faber.core.annotation.FaJob;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.NoDataException;
import com.faber.core.vo.utils.DictOption;
import com.faber.core.web.biz.BaseBiz;
import org.quartz.CronExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统定时任务
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2019-08-21 14:48:06
 */
@Service
public class JobBiz extends BaseBiz<JobMapper, Job> {

    @Autowired
    private JobTask jobTask;

    @Autowired private JobLogMapper jobLogMapper;
    @Autowired private JobDefinitionVersions definitionVersions;
    @Autowired private RegisteredJobService registeredJobs;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(Job job) {
        boolean saved = super.save(job);
        if (saved) recordDefinitionVersion(job);
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateById(Job job) {
        if (job.getId() == null || baseMapper.selectOne(new QueryWrapper<Job>().eq("id", job.getId()).last("FOR UPDATE")) == null)
            return false;
        boolean saved = super.updateById(job);
        if (saved) recordDefinitionVersion(job);
        return saved;
    }

    private void recordDefinitionVersion(Job input) {
        Job saved = baseMapper.selectOne(new QueryWrapper<Job>().eq("id", input.getId()).last("FOR UPDATE"));
        if (saved != null && Integer.valueOf(1).equals(saved.getJobType()))
            input.setCurrentDefinitionVersionId(definitionVersions.ensureCurrent(saved).getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdate(Job job) {
        if (job.getId() != null && baseMapper.selectOne(new QueryWrapper<Job>().eq("id", job.getId()).last("FOR UPDATE")) != null)
            return updateById(job);
        return save(job);
    }

    @Override
    protected void saveBefore(Job job) {
        Job old = job.getId() == null ? null : getById(job.getId());
        int oldType = old == null || old.getJobType() == null ? 0 : old.getJobType();
        int type = job.getJobType() == null ? oldType : job.getJobType();
        if (type != 0 && type != 1) throw new BuzzException("任务类型无效");
        if (old != null && type != oldType) throw new BuzzException("创建后不能切换任务类型");
        job.setJobType(type);
        if (old == null && job.getStatus() == null) job.setStatus(false);
        if (type == 1) {
            String name = job.getJobName() != null ? job.getJobName() : old == null ? null : old.getJobName();
            if (name == null || name.isBlank() || name.length() > 100) throw new BuzzException("流程名称须为1–100字符");
            String definition = job.getFlowDefinition() != null ? job.getFlowDefinition() : old == null ? null : old.getFlowDefinition();
            JobFlowDefinitionValidator.validate(definition, registeredFlowClasses());
            boolean enabled = old != null && Boolean.TRUE.equals(old.getStatus());
            if (job.getStatus() != null && Boolean.TRUE.equals(job.getStatus()) != enabled)
                throw new BuzzException("请通过启用/暂停调度操作修改流程调度状态");
            if (enabled && job.getCron() != null && !java.util.Objects.equals(job.getCron(), old.getCron()))
                throw new BuzzException("请先暂停调度，再修改Cron");
            String cron = job.getCron() != null ? job.getCron() : old == null ? null : old.getCron();
            if (cron == null || !CronExpression.isValidExpression(cron)) throw new BuzzException("Cron表达式无效");
            job.setFlowDefinition(definition);
            job.setClazzPath("");
            job.setStatus(enabled);
        } else {
            if (job.getFlowDefinition() != null && !job.getFlowDefinition().isBlank()) throw new BuzzException("普通任务不能保存流程定义");
            if (old == null && (job.getClazzPath() == null || job.getClazzPath().isBlank())) throw new BuzzException("请选择任务执行类");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<Job> jobs) {
        return saveBatch(jobs, DEFAULT_IMPORT_BATCH_SIZE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<Job> jobs) {
        return updateBatchById(jobs, DEFAULT_IMPORT_BATCH_SIZE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(Collection<Job> jobs) {
        return saveOrUpdateBatch(jobs, DEFAULT_IMPORT_BATCH_SIZE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveBatch(Collection<Job> jobs, int batchSize) {
        for (Job job : jobs) if (!save(job)) throw new BuzzException("批量保存任务失败");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateBatchById(Collection<Job> jobs, int batchSize) {
        for (Job job : jobs) if (!updateById(job)) throw new BuzzException("批量更新任务失败");
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatch(Collection<Job> jobs, int batchSize) {
        for (Job job : jobs) if (!saveOrUpdate(job)) throw new BuzzException("批量保存任务失败");
        return true;
    }

    private Set<String> registeredFlowClasses() {
        return RegisteredJobService.registeredPaths();
    }

    public JobFlowPreflight preflightFlow(Integer id) {
        Job job = getById(id);
        if (job == null) throw new NoDataException();
        if (!Integer.valueOf(1).equals(job.getJobType())) throw new BuzzException("仅流程任务支持运行前检查");
        return JobFlowPreflight.inspect(job.getId(), job.getJobName(), job.getFlowDefinition(), registeredFlowClasses());
    }

    private void requireOrdinary(Job job) {
        if (Integer.valueOf(1).equals(job.getJobType())) throw new BuzzException("流程任务请使用编排页面的执行入口");
    }

    private void lockAndCheckRemoval(Serializable id) {
        Job removing = baseMapper.selectOne(new QueryWrapper<Job>().eq("id", id).last("FOR UPDATE"));
        if (removing != null && Integer.valueOf(1).equals(removing.getJobType()) && Boolean.TRUE.equals(removing.getStatus()))
            throw new BuzzException("请先暂停流程调度，再删除");
        if (jobLogMapper.selectCount(new QueryWrapper<JobLog>().eq("job_id", id).eq("job_type", 1).in("status", 1, 3, 8)) > 0)
            throw new BuzzException("流程仍有执行中或未确认的运行，不能删除");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) { lockAndCheckRemoval(id); return super.removeById(id); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeBatchByIds(List<Serializable> ids) {
        if (ids == null || ids.isEmpty()) return;
        ids.stream().sorted(Comparator.comparing(Object::toString)).forEach(this::lockAndCheckRemoval);
        super.removeBatchByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePerById(Serializable id) { lockAndCheckRemoval(id); super.removePerById(id); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePerByIds(Collection<? extends Serializable> ids) {
        if (ids == null || ids.isEmpty()) return;
        ids.stream().sorted(Comparator.comparing(Object::toString)).forEach(this::lockAndCheckRemoval);
        super.removePerByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePerBatchByIds(List<Serializable> ids) { removePerByIds(ids); }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByQuery(QueryParams query) {
        if (count(getQueryWrapper(query)) > 1000) throw new BuzzException("删除数据超过1000条，请使用批量删除");
        removeBatchByIds(list(parseQuery(query)).stream().map(job -> (Serializable) job.getId()).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeMine() {
        removeBatchByIds(list(new QueryWrapper<Job>().eq("crt_user", getCurrentUserId())).stream().map(job -> (Serializable) job.getId()).toList());
    }

    public void runOneTime(Long id) {
        Job job = getById(id);
        if (job == null) {
            throw new NoDataException();
        }
        requireOrdinary(job);
        jobTask.runTaskImmediately(job);
    }

    @Transactional(rollbackFor = Exception.class)
    public void startJob(Integer id) {
        Job job = baseMapper.selectOne(new QueryWrapper<Job>().eq("id", id).last("FOR UPDATE"));
        if (job == null) {
            throw new NoDataException();
        }
        if (Integer.valueOf(1).equals(job.getJobType())) {
            JobFlowDefinitionValidator.validate(job.getFlowDefinition(), registeredFlowClasses());
            if (job.getCron() == null || !CronExpression.isValidExpression(job.getCron())) throw new BuzzException("Cron表达式无效");
        }
        if (!jobTask.startJob(job)) throw new BuzzException("启用调度失败，请检查Cron及调度器状态");
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) jobTask.remove(job);
            }
        });
        Job patch = new Job(); patch.setDetailLogEnabled(null); patch.setId(id); patch.setStatus(true);
        if (baseMapper.updateById(patch) != 1)
            throw new BuzzException("更新调度状态失败");
    }

    @Transactional(rollbackFor = Exception.class)
    public void endJob(Integer id) {
        Job job = baseMapper.selectOne(new QueryWrapper<Job>().eq("id", id).last("FOR UPDATE"));
        if (job == null) {
            throw new NoDataException();
        }

        boolean registered = jobTask.checkJob(job);
        if (registered && !jobTask.remove(job)) throw new BuzzException("暂停调度失败，请重试");
        if (registered) org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) jobTask.startJob(job);
            }
        });
        Job patch = new Job(); patch.setDetailLogEnabled(null); patch.setId(id); patch.setStatus(false);
        if (baseMapper.updateById(patch) != 1)
            throw new BuzzException("更新调度状态失败");
    }

    public List<String> quartzLatest(String cron, Integer times) throws ParseException  {
        if (times > 20) {
            throw new BuzzException("times should less then 20.");
        }

        CronExpression cronExpression = new CronExpression(cron);
        List<String> nextFireTimes = new ArrayList<>();
        Date date = new Date();
        for (int i = 0; i < times; i++) {
            date = cronExpression.getNextValidTimeAfter(date);
            nextFireTimes.add(DateUtil.formatDateTime(date));
        }
        return nextFireTimes;
    }

    public List<Job> getStartUpJobs() {
        return lambdaQuery().eq(Job::getStatus, true).list();
    }

    public List<RegisteredJobVo> getRegisteredJobs() { return registeredJobs.catalog(); }

    public List<DictOption<String>> getAllJobs() {
        return RegisteredJobService.registeredTypes()
                .stream()
                .map(clazz -> new DictOption<String>(clazz.getName(), clazz.getAnnotation(FaJob.class).value()))
                .sorted(Comparator.comparing(DictOption::getLabel))
                .collect(Collectors.toList());
    }

    public List<DictOption<String>> getIdleJobs() {
        List<Job> activeJobs = lambdaQuery().list();
        return RegisteredJobService.registeredTypes()
                .stream()
                .filter(i -> activeJobs.stream().noneMatch(j -> ObjUtil.equals(j.getClazzPath(), i.getName())))
                .map(clazz -> new DictOption<String>(clazz.getName(), clazz.getAnnotation(FaJob.class).value()))
                .sorted(Comparator.comparing(DictOption::getLabel))
                .collect(Collectors.toList());
    }

}
