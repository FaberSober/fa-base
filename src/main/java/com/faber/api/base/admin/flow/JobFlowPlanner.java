package com.faber.api.base.admin.flow;

import com.faber.api.base.admin.entity.JobFlowNodeRun;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;

/** 纯依赖决策：状态持久化及互斥由运行存储层负责。 */
public final class JobFlowPlanner {
    private JobFlowPlanner() {}
    public record Decision(List<String> blocked, String next) {}
    public static Decision next(JsonNode definition, List<JobFlowNodeRun> runs) {
        Map<String, Integer> states = new HashMap<>();
        Map<String, String> classes = new HashMap<>();
        runs.forEach(run -> states.put(run.getNodeId(), run.getStatus()));
        runs.forEach(run -> classes.put(run.getNodeId(), run.getClazzPath()));
        Map<String, List<String>> parents = new LinkedHashMap<>();
        definition.path("nodes").forEach(node -> parents.put(node.path("id").asText(), new ArrayList<>()));
        definition.path("edges").forEach(edge -> parents.get(edge.path("target").asText()).add(edge.path("source").asText()));
        List<String> blocked = new ArrayList<>();
        boolean changed;
        do {
            changed = false;
            for (var node : parents.entrySet()) {
                if (waiting(states.get(node.getKey())) && node.getValue().stream().anyMatch(id -> Set.of(50, 60, 70, 80).contains(states.get(id)))) {
                    states.put(node.getKey(), 60); blocked.add(node.getKey()); changed = true;
                }
            }
        } while (changed);
        // 未确认工作可能仍在产生副作用，安全暂停全部新领取。
        if (states.containsValue(80)) {
            parents.keySet().stream().filter(id -> waiting(states.get(id))).forEach(blocked::add);
            return new Decision(blocked, null);
        }
        int limit = Math.max(1, Math.min(20, definition.path("maxConcurrency").asInt(1)));
        Set<String> busyClasses = new HashSet<>();
        int active = 0;
        for (JobFlowNodeRun run : runs) if (Set.of(20, 30, 80).contains(run.getStatus())) {
            active++; busyClasses.add(run.getClazzPath());
        }
        if (active >= limit) return new Decision(blocked, null);
        String next = parents.entrySet().stream().filter(node -> waiting(states.get(node.getKey()))
                        && !busyClasses.contains(classes.get(node.getKey()))
                        && node.getValue().stream().allMatch(id -> states.get(id) == 40))
                .map(Map.Entry::getKey).findFirst().orElse(null);
        return new Decision(blocked, next);
    }
    private static boolean waiting(Integer state) { return Integer.valueOf(0).equals(state) || Integer.valueOf(10).equals(state); }
}
