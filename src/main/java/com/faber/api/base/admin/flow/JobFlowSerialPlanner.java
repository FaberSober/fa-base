package com.faber.api.base.admin.flow;

import com.faber.api.base.admin.entity.JobFlowNodeRun;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** 纯依赖决策：状态持久化及互斥由运行存储层负责。 */
public final class JobFlowSerialPlanner {
    private JobFlowSerialPlanner() {}
    public record Decision(List<String> blocked, String next) {}
    public static Decision next(JsonNode definition, List<JobFlowNodeRun> runs) {
        Map<String, Integer> states = new HashMap<>();
        runs.forEach(run -> states.put(run.getNodeId(), run.getStatus()));
        Map<String, List<String>> parents = new LinkedHashMap<>();
        definition.path("nodes").forEach(node -> parents.put(node.path("id").asText(), new ArrayList<>()));
        definition.path("edges").forEach(edge -> parents.get(edge.path("target").asText()).add(edge.path("source").asText()));
        List<String> blocked = new ArrayList<>();
        boolean changed;
        do {
            changed = false;
            for (var node : parents.entrySet()) {
                if (states.get(node.getKey()) == 0 && node.getValue().stream().anyMatch(id -> Set.of(50, 60, 70, 80).contains(states.get(id)))) {
                    states.put(node.getKey(), 60); blocked.add(node.getKey()); changed = true;
                }
            }
        } while (changed);
        // 未确认工作仍占用本阶段唯一串行容量，不能放行独立分支。
        if (states.containsValue(80)) {
            parents.keySet().stream().filter(id -> states.get(id) == 0).forEach(blocked::add);
            return new Decision(blocked, null);
        }
        if (states.values().stream().anyMatch(state -> state == 20 || state == 30)) return new Decision(blocked, null);
        String next = parents.entrySet().stream().filter(node -> states.get(node.getKey()) == 0 && node.getValue().stream().allMatch(id -> states.get(id) == 40))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        return new Decision(blocked, next);
    }
}
