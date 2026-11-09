package com.faber.config.quartz;

import com.faber.api.base.admin.flow.*;
import com.faber.api.base.admin.entity.*;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.core.context.BaseContextHandler;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.concurrent.Executor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobFlowScanTest {
    private JobLog record(int id) { JobLog r = new JobLog(); r.setId(id); r.setJobId(4); return r; }
    private JobFlowRunView view(int id, int nodeStatus) throws Exception {
        JobLog r = record(id); r.setStatus(JobLogStatusEnum.DOING);
        JobFlowNodeRun n = new JobFlowNodeRun(); n.setNodeId("a"); n.setClazzPath("test.Task"); n.setStatus(nodeStatus);
        return new JobFlowRunView(r, List.of(n), new ObjectMapper().readTree("{\"nodes\":[{\"id\":\"a\"}],\"edges\":[],\"maxConcurrency\":1}"));
    }
    private JobFlowRunner runner(JobFlowRunStore store, Executor executor) {
        var runner = new JobFlowRunner(); ReflectionTestUtils.setField(runner, "store", store);
        ReflectionTestUtils.setField(runner, "executor", executor); return runner;
    }
    @Test void waitsForRecoveryAndDoesNotQueueUnchangedRunningWork() throws Exception {
        var store = mock(JobFlowRunStore.class); var executor = mock(Executor.class); var runner = runner(store, executor);
        runner.reconcileActiveRuns(); verifyNoInteractions(store);
        ReflectionTestUtils.setField(runner, "ready", true);
        when(store.activeRunPage(0)).thenReturn(List.of(record(1))); when(store.reconcile(1, 4)).thenReturn(view(1, 30));
        runner.reconcileActiveRuns(); verifyNoInteractions(executor); assertNull(BaseContextHandler.getUserId());
    }
    @Test void rotatesCursorAndIsolatesBadRecordsAndQueryFailures() throws Exception {
        var store = mock(JobFlowRunStore.class); var runner = runner(store, mock(Executor.class));
        ReflectionTestUtils.setField(runner, "ready", true);
        when(store.activeRunPage(0)).thenThrow(new IllegalStateException("db")).thenReturn(List.of(record(1), record(2)));
        when(store.reconcile(1, 4)).thenThrow(new IllegalStateException("bad record")); when(store.reconcile(2, 4)).thenReturn(view(2, 30));
        when(store.activeRunPage(2)).thenReturn(List.of());
        runner.reconcileActiveRuns(); runner.reconcileActiveRuns(); verify(store).reconcile(2, 4);
        runner.reconcileActiveRuns(); runner.reconcileActiveRuns(); verify(store, times(2)).reconcile(2, 4);
    }
    @Test void doesNotAccumulateQueuedCompensationAndRetriesRejectedSubmission() throws Exception {
        var store = mock(JobFlowRunStore.class); var executor = mock(Executor.class); var runner = runner(store, executor);
        ReflectionTestUtils.setField(runner, "ready", true);
        when(store.activeRunPage(0)).thenReturn(List.of(record(1))); when(store.activeRunPage(1)).thenReturn(List.of());
        when(store.reconcile(1, 4)).thenReturn(view(1, 40));
        runner.reconcileActiveRuns(); runner.reconcileActiveRuns(); runner.reconcileActiveRuns();
        var action = org.mockito.ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(action.capture()); verify(store).reconcile(1, 4);
        action.getValue().run(); runner.reconcileActiveRuns(); runner.reconcileActiveRuns();
        verify(executor, times(2)).execute(any());
        var rejected = mock(Executor.class); doThrow(new java.util.concurrent.RejectedExecutionException()).when(rejected).execute(any());
        var retry = runner(store, rejected); ReflectionTestUtils.setField(retry, "ready", true);
        retry.reconcileActiveRuns(); retry.reconcileActiveRuns(); retry.reconcileActiveRuns(); verify(rejected, times(2)).execute(any());
    }
}
