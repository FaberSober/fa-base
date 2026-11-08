package com.faber.api.base.admin.biz;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.faber.api.base.admin.entity.JobFlowNodeRun;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.api.base.admin.flow.JobFlowRunStore;
import com.faber.api.base.admin.flow.JobFlowSerialPlanner;
import com.faber.api.base.admin.mapper.JobFlowNodeRunMapper;
import com.faber.api.base.admin.mapper.JobLogMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobFlowSerialRunTest {
    private JsonNode definition() throws Exception {
        return new ObjectMapper().readTree("""
          {"nodes":[{"id":"a"},{"id":"b"},{"id":"c"},{"id":"independent"}],
           "edges":[{"source":"a","target":"b"},{"source":"b","target":"c"}]}
          """);
    }
    private JobFlowNodeRun node(String id, int state) { var node = new JobFlowNodeRun(); node.setNodeId(id); node.setStatus(state); return node; }
    @Test void mainMethodReturnDoesNotReleaseExecutingAsyncNode() throws Exception {
        var decision = JobFlowSerialPlanner.next(definition(), List.of(node("a", 30), node("b", 0), node("c", 0), node("independent", 0)));
        assertNull(decision.next()); assertTrue(decision.blocked().isEmpty());
        assertEquals("b", JobFlowSerialPlanner.next(definition(), List.of(node("a", 40), node("b", 0), node("c", 0), node("independent", 0))).next());
    }
    @Test void failedAncestorsBlockDescendantsButNotIndependentBranch() throws Exception {
        for (int status : List.of(50)) {
            var decision = JobFlowSerialPlanner.next(definition(), List.of(node("a", status), node("b", 0), node("c", 0), node("independent", 0)));
            assertEquals(List.of("b", "c"), decision.blocked()); assertEquals("independent", decision.next());
        }
    }
    @Test void unconfirmedWorkRetainsSerialCapacityAndBlocksEveryUnstartedNode() throws Exception {
        var decision = JobFlowSerialPlanner.next(definition(), List.of(node("a",80), node("b",0), node("c",0), node("independent",0)));
        assertNull(decision.next()); assertEquals(List.of("b","c","independent"), decision.blocked());
    }
    @Test void decisionPreservesSnapshotAndNodeRecords() throws Exception {
        JsonNode snapshot = definition(); JsonNode before = snapshot.deepCopy();
        var runs = List.of(node("a", 50), node("b", 0), node("c", 0), node("independent", 0));
        JobFlowSerialPlanner.next(snapshot, runs);
        assertEquals(before, snapshot); assertEquals(0, runs.get(1).getStatus()); assertEquals(0, runs.get(2).getStatus());
    }
    @Test void repeatedCompletionCallbackCannotAdvanceTwice() {
        var store = new JobFlowRunStore(); var nodes = mock(JobFlowNodeRunMapper.class); var logs = mock(JobLogMapper.class);
        ReflectionTestUtils.setField(store, "nodes", nodes); ReflectionTestUtils.setField(store, "logs", logs);
        var run = node("a", 30); run.setId(1); run.setFlowLogId(2); run.setJobLogId(3);
        when(nodes.selectOne(any(Wrapper.class))).thenReturn(run);
        when(nodes.update(isNull(), any(Wrapper.class))).thenReturn(1, 0);
        var business = new JobLog(); business.setId(3); business.setStatus(JobLogStatusEnum.DONE);
        assertTrue(store.completeNode(business)); assertFalse(store.completeNode(business));
    }
    @Test void claimedButNotStartedNodeBlocksAllNewClaims() throws Exception {
        assertNull(JobFlowSerialPlanner.next(definition(), List.of(node("a", 20), node("b", 0), node("c", 0), node("independent", 0))).next());
    }
    @Test void flowAndNodeBusinessLogsCannotBeDeletedEvenAfterCompletion() {
        JobLogBiz biz = spy(new JobLogBiz()); var nodes = mock(JobFlowNodeRunMapper.class);
        ReflectionTestUtils.setField(biz, "flowNodeRuns", nodes);
        JobLog parent = new JobLog(); parent.setId(1); parent.setJobType(1); parent.setStatus(JobLogStatusEnum.DONE);
        doReturn(parent).when(biz).getById(1);
        assertThrows(com.faber.core.exception.BuzzException.class, () -> biz.removeById(1));
        JobLog child = new JobLog(); child.setId(2); child.setJobType(0); child.setStatus(JobLogStatusEnum.DONE);
        doReturn(child).when(biz).getById(2);
        when(nodes.selectCount(any(Wrapper.class))).thenReturn(1L);
        assertThrows(com.faber.core.exception.BuzzException.class, () -> biz.removeById(2));
    }

}
