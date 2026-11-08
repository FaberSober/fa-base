package com.faber.api.base.admin.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.faber.core.exception.BuzzException;
import java.util.*;

/** 只读预检结果；不领取节点、不创建日志，也不提交业务执行。 */
public record JobFlowPreflight(Integer jobId, String jobName, int maxConcurrency, int nodeCount, int edgeCount,
                               List<List<Node>> layers, List<String> repeatedClasses) {
    public record Node(String id, String name, String clazzPath, List<String> predecessors) {}

    public static JobFlowPreflight inspect(Integer jobId, String name, String source, Set<String> registered) {
        JobFlowDefinitionValidator.validate(source, registered);
        try {
            JsonNode root = new ObjectMapper().readTree(source);
            Map<String, JsonNode> nodes = new LinkedHashMap<>();
            Map<String, List<String>> predecessors = new HashMap<>();
            Map<String, Integer> classCounts = new LinkedHashMap<>();
            for (JsonNode node : root.path("nodes")) {
                String id = node.path("id").asText();
                nodes.put(id, node);
                predecessors.put(id, new ArrayList<>());
                classCounts.merge(node.path("clazzPath").asText(), 1, Integer::sum);
            }
            for (JsonNode edge : root.path("edges")) predecessors.get(edge.path("target").asText()).add(edge.path("source").asText());
            Set<String> completed = new HashSet<>();
            List<List<Node>> layers = new ArrayList<>();
            while (completed.size() < nodes.size()) {
                List<Node> layer = new ArrayList<>();
                nodes.forEach((id, node) -> {
                    if (!completed.contains(id) && completed.containsAll(predecessors.get(id)))
                        layer.add(new Node(id, node.path("name").asText(), node.path("clazzPath").asText(), List.copyOf(predecessors.get(id))));
                });
                if (layer.isEmpty()) throw new BuzzException("流程依赖无法推进");
                layers.add(List.copyOf(layer));
                layer.forEach(node -> completed.add(node.id()));
            }
            return new JobFlowPreflight(jobId, name, root.path("maxConcurrency").asInt(), nodes.size(), root.path("edges").size(),
                    List.copyOf(layers), classCounts.entrySet().stream().filter(entry -> entry.getValue() > 1).map(Map.Entry::getKey).toList());
        } catch (java.io.IOException e) { throw new BuzzException("流程JSON格式无效"); }
    }
}
