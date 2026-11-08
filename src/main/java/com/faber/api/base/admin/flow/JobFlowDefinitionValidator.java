package com.faber.api.base.admin.flow;

import com.faber.core.exception.BuzzException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 不加载客户端指定的类，只接受服务端扫描得到的注册类白名单。 */
public final class JobFlowDefinitionValidator {
    private static final ObjectMapper JSON = new ObjectMapper();
    private JobFlowDefinitionValidator() {}

    public static void validate(String source, Set<String> registered) {
        if (source == null || source.getBytes(StandardCharsets.UTF_8).length > 256 * 1024) fail("流程定义不能为空或超过256KB");
        try {
            JsonNode root = JSON.readTree(source);
            if (root == null || !root.isObject() || !root.path("schemaVersion").isIntegralNumber() || !root.path("schemaVersion").canConvertToInt() || root.path("schemaVersion").intValue() != 1) fail("流程版本无效");
            text(root.path("name"), "流程名称");
            JsonNode concurrency = root.path("maxConcurrency");
            if (!concurrency.isIntegralNumber() || !concurrency.canConvertToInt() || concurrency.intValue() < 1 || concurrency.intValue() > 20) fail("并发上限必须为1–20整数");
            JsonNode nodes = root.path("nodes"), edges = root.path("edges");
            if (!nodes.isArray() || nodes.isEmpty() || nodes.size() > 100 || !edges.isArray() || edges.size() > 500) fail("流程须包含1–100个节点、最多500条边");
            Map<String, Integer> degree = new LinkedHashMap<>();
            Map<String, List<String>> children = new HashMap<>();
            for (JsonNode node : nodes) {
                String id = text(node.path("id"), "节点ID");
                if (degree.putIfAbsent(id, 0) != null) fail("节点ID重复");
                children.put(id, new ArrayList<>());
                text(node.path("name"), "节点名称");
                if (!node.path("clazzPath").isTextual() || !registered.contains(node.path("clazzPath").textValue())) fail("节点任务类未注册");
                for (String axis : List.of("x", "y")) {
                    JsonNode coordinate = node.path("position").path(axis);
                    if (!coordinate.isNumber() || !Double.isFinite(coordinate.doubleValue()) || Math.abs(coordinate.doubleValue()) > 1000000) fail("节点坐标无效");
                }
            }
            Set<List<String>> pairs = new HashSet<>();
            for (JsonNode edge : edges) {
                String from = text(edge.path("source"), "连线起点"), to = text(edge.path("target"), "连线终点");
                if (!degree.containsKey(from) || !degree.containsKey(to) || from.equals(to) || !pairs.add(List.of(from, to))) fail("连线端点、自环或重复边无效");
                children.get(from).add(to);
                degree.put(to, degree.get(to) + 1);
            }
            Deque<String> ready = new ArrayDeque<>();
            degree.forEach((id, count) -> { if (count == 0) ready.add(id); });
            int visited = 0;
            while (!ready.isEmpty()) {
                String id = ready.remove(); visited++;
                for (String next : children.get(id)) if (degree.compute(next, (key, count) -> count - 1) == 0) ready.add(next);
            }
            if (visited != nodes.size()) fail("流程存在循环依赖");
        } catch (java.io.IOException e) { throw new BuzzException("流程JSON格式无效"); }
    }
    private static String text(JsonNode value, String label) {
        if (!value.isTextual() || value.textValue().isBlank() || value.textValue().length() > 100) fail(label + "须为1–100字符");
        return value.textValue();
    }
    private static void fail(String message) { throw new BuzzException(message); }
}
