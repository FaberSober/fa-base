package com.faber.api.base.admin.flow;

import cn.hutool.core.util.ClassUtil;
import com.faber.api.base.admin.biz.JobLogBiz;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.config.quartz.BaseJob;
import com.faber.core.annotation.FaJob;
import com.faber.core.config.thread.TaskExecutionTracker;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/** 单实例、持久状态领取的串行执行器；不等待业务异步线程，不在事务内调用任务。 */
@Slf4j
@Service
public class JobFlowRunner {
    private volatile boolean ready;
    @Resource private JobFlowRunStore store;
    @Resource private JobLogBiz logBiz;
    @Resource(name = "executor") private Executor executor;
    @Resource private AutowireCapableBeanFactory beanFactory;

    public JobFlowRunView start(Integer jobId) {
        if (!ready) throw new com.faber.core.exception.BuzzException("流程运行恢复检查尚未完成，请稍后再试");
        JobFlowRunView view = store.begin(jobId);
        enqueue(view.log().getId());
        return view;
    }
    public JobFlowRunView view(Integer logId) { return store.view(logId); }
    public java.util.List<JobLog> history(Integer jobId) { return store.history(jobId); }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void recover() { if (ready) return; store.recoverInterrupted(); ready = true; }

    private void enqueue(Integer flowLogId) {
        try (var ignored = TaskExecutionTracker.bind(null)) {
            if (executor instanceof ThreadPoolTaskExecutor pool && pool.getThreadPoolExecutor().isShutdown()) throw new RejectedExecutionException("执行器已关闭");
            executor.execute(() -> {
                try { advance(flowLogId); }
                catch (Throwable error) { log.error("流程推进失败，保留持久状态禁止盲目重跑，运行ID：{}", flowLogId, error); }
            });
        } catch (RuntimeException error) {
            store.failSubmission(flowLogId, "执行器未接受推进请求：" + summary(error));
        }
    }
    private void advance(Integer flowLogId) {
        JobFlowRunStore.Launch launch = store.claim(flowLogId);
        if (launch == null) return;
        BaseJob task = null;
        boolean invoked = false;
        try {
            Class<? extends BaseJob> type = ClassUtil.scanPackageByAnnotation("com.faber", FaJob.class).stream()
                    .filter(BaseJob.class::isAssignableFrom).filter(clazz -> clazz.getName().equals(launch.node().getClazzPath()))
                    .findFirst().orElseThrow(() -> new IllegalStateException("快照引用的任务类已不可用")).asSubclass(BaseJob.class);
            task = beanFactory.createBean(type);
            BaseJob instance = task;
            if (!store.markRunning(launch.node().getId())) { destroy(instance); return; }
            invoked = true;
            instance.executeFlowNode(launch.log(), launch.detailLogEnabled(), record -> {
                try {
                    if (store.completeNode(record)) enqueue(flowLogId);
                } finally { destroy(instance); }
            });
        } catch (Throwable error) {
            // Error 可能已被生命周期收尾并回调，不能覆盖其已提交结果。
            JobLog current = logBiz.getById(launch.log().getId());
            if (current != null && current.getStatus() == JobLogStatusEnum.DOING) {
                current.setStatus(invoked ? JobLogStatusEnum.UNCONFIRMED : JobLogStatusEnum.ERROR);
                current.setTrackingVersion(invoked ? 2 : 1); current.setEndTime(invoked ? null : new Date());
                current.setDurationMs(invoked ? null : Math.max(0, current.getEndTime().getTime() - current.getBeginTime().getTime()));
                current.setDuration(invoked ? 0L : current.getDurationMs() / 1000);
                current.setErrMsg((invoked ? "节点启动结果无法确认：" : "节点启动失败：") + summary(error));
                if (logBiz.finishExecution(current) && store.completeNode(current)) enqueue(flowLogId);
                if (task != null && !invoked) destroy(task);
            }
            log.error("流程节点启动异常，运行ID：{}，节点：{}", flowLogId, launch.node().getNodeId(), error);
        }
    }
    private void destroy(BaseJob task) {
        try { beanFactory.destroyBean(task); } catch (Exception error) { log.warn("清理节点实例失败", error); }
    }
    private static String summary(Throwable error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return message.substring(0, Math.min(1800, message.length()));
    }
}
