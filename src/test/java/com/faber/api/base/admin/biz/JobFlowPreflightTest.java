package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.flow.JobFlowPreflight;
import com.faber.core.exception.BuzzException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobFlowPreflightTest {
    private String source(List<Map<String, String>> edges) throws Exception {
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (String id : List.of("a", "b", "c", "d")) nodes.add(Map.of("id", id, "name", id, "clazzPath", "com.example.Job", "position", Map.of("x", 0, "y", 0)));
        return new ObjectMapper().writeValueAsString(Map.of("schemaVersion", 1, "name", "流程", "maxConcurrency", 2, "nodes", nodes, "edges", edges));
    }
    private Map<String, String> edge(String from, String to) { return Map.of("source", from, "target", to); }
    @Test void previewsMultipleEntriesAndConvergingDependencies() throws Exception {
        var result = JobFlowPreflight.inspect(12, "已保存名称", source(List.of(edge("a", "c"), edge("b", "c"), edge("c", "d"))), Set.of("com.example.Job"));
        assertEquals(List.of(List.of("a", "b"), List.of("c"), List.of("d")), result.layers().stream().map(layer -> layer.stream().map(JobFlowPreflight.Node::id).toList()).toList());
        assertEquals(List.of("a", "b"), result.layers().get(1).get(0).predecessors());
        assertEquals(4, result.nodeCount()); assertEquals(3, result.edgeCount()); assertEquals(2, result.maxConcurrency());
        assertEquals("已保存名称", result.jobName());
        assertEquals(List.of("com.example.Job"), result.repeatedClasses());
    }
    @Test void allowsIndependentNodesAndStableDefinitionOrder() throws Exception {
        var result = JobFlowPreflight.inspect(1, "流程", source(List.of()), Set.of("com.example.Job"));
        assertEquals(1, result.layers().size());
        assertEquals(List.of("a", "b", "c", "d"), result.layers().get(0).stream().map(JobFlowPreflight.Node::id).toList());
    }
    @Test void rejectsUnavailableClassesAndCycles() throws Exception {
        String unavailable = source(List.of());
        assertThrows(BuzzException.class, () -> JobFlowPreflight.inspect(1, "流程", unavailable, Set.of()));
        String cycle = source(List.of(edge("a", "b"), edge("b", "a")));
        assertThrows(BuzzException.class, () -> JobFlowPreflight.inspect(1, "流程", cycle, Set.of("com.example.Job")));
    }
    @Test void bizRejectsOrdinaryTasksWithoutWrites() {
        JobBiz biz = spy(new JobBiz()); Job job = new Job(); job.setJobType(0);
        doReturn(job).when(biz).getById(1);
        assertThrows(BuzzException.class, () -> biz.preflightFlow(1));
        verify(biz, never()).save(any(Job.class));
        verify(biz, never()).updateById(any(Job.class));
    }
}
