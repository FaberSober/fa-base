package com.faber.api.base.admin.flow;

import com.faber.core.vo.msg.TableRet;
import com.faber.api.base.admin.vo.req.JobFlowHistoryQuery;
import com.faber.api.base.admin.biz.JobLogBiz;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.config.quartz.BaseJob;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** 单实例并行执行器；按持久状态领取，不等待业务异步线程，不在事务内调用任务。 */
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
    public JobFlowRunView view(Integer logId, Integer jobId) { return store.view(logId, jobId); }
    public TableRet<JobLog> historyPage(Integer jobId, JobFlowHistoryQuery request) { return store.historyPage(jobId, request); }
    public java.util.List<JobLog> history(Integer jobId) { return store.history(jobId); }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void recover() { if (ready) return; store.recoverInterrupted(); ready = true; }

    private void enqueue(Integer flowLogId) {
        submit(() -> {
            try { advance(flowLogId); }
            catch (Throwable error) { log.error("流程推进失败，保留持久状态禁止盲目重跑，运行ID：{}", flowLogId, error); }
        }, error -> store.failSubmission(flowLogId, "执行器未接受推进请求：" + summary(error)));
    }
    private void advance(Integer flowLogId) {
        JobFlowRunStore.Launch launch;
        // claim 在父运行行锁下登记启动中，排队任务也占容量；并发推进不会重复领取。
        while ((launch = store.claim(flowLogId)) != null) {
            JobFlowRunStore.Launch claimed = launch;
            submit(() -> executeNode(flowLogId, claimed), error -> {
                // execute未接受，业务尚未调用，可以确定为启动失败；继续检查其他独立分支。
                if (finishLaunch(claimed, false, "执行器未接受节点：" + summary(error))) enqueue(flowLogId);
            });
        }
    }
    private void submit(Runnable action, Consumer<Throwable> rejected) {
        AtomicBoolean started = new AtomicBoolean();
        AtomicBoolean reported = new AtomicBoolean();
        Consumer<Throwable> report = error -> {
            if (reported.compareAndSet(false, true)) rejected.accept(error);
        };
        // 独立跟踪编排投递，接收共享执行器的关闭丢弃/排队取消通知；不挂到业务节点生命周期。
        TaskExecutionTracker dispatch = new TaskExecutionTracker(() -> () -> {}, result -> {
            if (!started.get() && result.failure() != null) report.accept(result.failure());
        });
        Throwable failure = null;
        try (var ignored = TaskExecutionTracker.bind(dispatch)) {
            if (executor instanceof ThreadPoolTaskExecutor pool && pool.getThreadPoolExecutor().isShutdown())
                throw new RejectedExecutionException("执行器已关闭");
            executor.execute(() -> {
                started.set(true);
                // 实际业务自己创建跟踪器，推进/收尾也不得登记为编排的子业务。
                try (var scope = TaskExecutionTracker.bind(null)) { action.run(); }
            });
        } catch (RuntimeException | Error error) {
            failure = error;
            if (!started.get()) report.accept(error);
            else throw error;
        } finally {
            dispatch.completeMain(failure);
        }
    }
    private void executeNode(Integer flowLogId, JobFlowRunStore.Launch launch) {
        BaseJob task = null;
        boolean invoked = false;
        try {
            Class<? extends BaseJob> type = RegisteredJobService.registeredTypes().stream().filter(clazz -> clazz.getName().equals(launch.node().getClazzPath()))
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
            if (finishLaunch(launch, invoked, (invoked ? "节点启动结果无法确认：" : "节点启动失败：") + summary(error))) enqueue(flowLogId);
            if (task != null && !invoked) destroy(task);
            log.error("流程节点启动异常，运行ID：{}，节点：{}", flowLogId, launch.node().getNodeId(), error);
        }
    }
    private boolean finishLaunch(JobFlowRunStore.Launch launch, boolean invoked, String reason) {
        JobLog current = logBiz.getById(launch.log().getId());
        if (current == null || current.getStatus() != JobLogStatusEnum.DOING) return false;
        current.setStatus(invoked ? JobLogStatusEnum.UNCONFIRMED : JobLogStatusEnum.ERROR);
        current.setTrackingVersion(invoked ? 2 : 1); current.setEndTime(invoked ? null : new Date());
        current.setDurationMs(invoked ? null : Math.max(0, current.getEndTime().getTime() - current.getBeginTime().getTime()));
        current.setDuration(invoked ? 0L : current.getDurationMs() / 1000);
        current.setErrMsg(reason);
        return logBiz.finishExecution(current) && store.completeNode(current);
    }
    private void destroy(BaseJob task) {
        try { beanFactory.destroyBean(task); } catch (Exception error) { log.warn("清理节点实例失败", error); }
    }
    private static String summary(Throwable error) {
        String message = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return message.substring(0, Math.min(1800, message.length()));
    }
}
