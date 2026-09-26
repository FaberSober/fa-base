package com.faber.api.base.push.unipush;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.api.base.push.entity.PushDevice;
import com.faber.core.exception.BuzzException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Minimal UniPush REST API v2 client for one-device test notifications. */
@Service
public class UniPushRestClient {

    private static final long TOKEN_REFRESH_SKEW_MS = 60_000L;
    private static final long DEFAULT_TOKEN_TTL_MS = 23 * 60 * 60 * 1000L;
    private static final long MESSAGE_TTL_MS = 2 * 60 * 60 * 1000L;

    private final UniPushProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Object tokenLock = new Object();
    private volatile CachedToken cachedToken;

    @Autowired
    public UniPushRestClient(UniPushProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, createHttpClient(properties));
    }

    UniPushRestClient(UniPushProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public boolean isConfigured() {
        return properties.isConfigured();
    }

    public boolean supports(PushDevice device) {
        return device != null && properties.supports(
                new UniPushProperties.PushDeviceIdentity(device.getAppId(), device.getEnvironment()));
    }

    public UniPushDeliveryResult send(PushDevice device, String title, String body, String payloadJson) {
        if (!isConfigured()) {
            throw new BuzzException("UniPush 尚未配置服务端 AppID、AppKey 和 MasterSecret");
        }

        Map<String, Object> requestBody = buildPushBody(device, title, body, payloadJson);
        ApiResponse response = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            String token = getToken(attempt > 0);
            response = postJson("/push/single/cid", token, requestBody);
            int code = response.body().path("code").asInt(-1);
            if ((code == 10001 || response.httpStatus() == 401) && attempt == 0) {
                cachedToken = null;
                continue;
            }
            break;
        }

        if (response == null) {
            return failed("UniPush 未返回发送结果", false);
        }
        int code = response.body().path("code").asInt(-1);
        String providerMessage = response.body().path("msg").asText("");
        if (response.httpStatus() < 200 || response.httpStatus() >= 300 || code != 0) {
            boolean invalidCid = code == 20001 && isInvalidCidMessage(providerMessage);
            return failed(providerMessage.isBlank() ? "UniPush 请求失败" : providerMessage,
                    invalidCid, String.valueOf(code));
        }

        return parseDeliveryResult(response.body(), device.getClientId());
    }

    private Map<String, Object> buildPushBody(PushDevice device, String title, String body, String payloadJson) {
        Map<String, Object> notification = new LinkedHashMap<>();
        notification.put("title", title);
        notification.put("body", body);
        notification.put("click_type", "payload");
        notification.put("payload", payloadJson);

        Map<String, Object> pushMessage = Map.of("notification", notification);
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("request_id", java.util.UUID.randomUUID().toString().replace("-", ""));
        request.put("settings", Map.of("ttl", MESSAGE_TTL_MS));
        request.put("audience", Map.of("cid", List.of(device.getClientId())));
        request.put("push_message", pushMessage);
        request.put("push_channel", buildPushChannel(notification, title, body, payloadJson));
        return request;
    }

    private Map<String, Object> buildPushChannel(Map<String, Object> notification,
                                                  String title,
                                                  String body,
                                                  String payloadJson) {
        Map<String, Object> iosAlert = Map.of("title", title, "body", body);
        Map<String, Object> iosAps = Map.of("alert", iosAlert, "content-available", 0);
        Map<String, Object> ios = Map.of("type", "notify", "payload", payloadJson, "aps", iosAps);
        Map<String, Object> android = Map.of("ups", Map.of("notification", notification));
        return Map.of("ios", ios, "android", android);
    }

    private UniPushDeliveryResult parseDeliveryResult(JsonNode response, String clientId) {
        JsonNode data = response.path("data");
        if (!data.isObject()) {
            return failed("UniPush 响应缺少设备发送结果", false, "unknown");
        }

        var taskIterator = data.fields();
        if (!taskIterator.hasNext()) {
            return failed("UniPush 响应缺少任务编号", false, "unknown");
        }
        Map.Entry<String, JsonNode> task = taskIterator.next();
        String providerStatus = task.getValue().path(clientId).asText("");
        if (providerStatus.isBlank()) {
            return failed("UniPush 响应缺少设备发送状态", false, "unknown");
        }
        if ("successed_online".equals(providerStatus) || "successed_offline".equals(providerStatus)) {
            return new UniPushDeliveryResult("accepted", providerStatus, task.getKey(),
                    "UniPush 已受理，设备是否收到需等待客户端回执", false);
        }
        if ("successed_ignore".equals(providerStatus)) {
            return new UniPushDeliveryResult("ignored", providerStatus, task.getKey(),
                    "UniPush 未向该设备下发（近期不活跃）", false);
        }
        return new UniPushDeliveryResult("failed", providerStatus, task.getKey(),
                "UniPush 返回未识别的设备状态", false);
    }

    private String getToken(boolean forceRefresh) {
        long now = System.currentTimeMillis();
        CachedToken current = cachedToken;
        if (!forceRefresh && current != null && current.refreshAt() > now) {
            return current.value();
        }

        synchronized (tokenLock) {
            now = System.currentTimeMillis();
            current = cachedToken;
            if (!forceRefresh && current != null && current.refreshAt() > now) {
                return current.value();
            }

            long timestamp = now;
            String sign = sha256(properties.getAppKey() + timestamp + properties.getMasterSecret());
            ApiResponse response = postJson("/auth", null, Map.of(
                    "sign", sign,
                    "timestamp", String.valueOf(timestamp),
                    "appkey", properties.getAppKey()));
            JsonNode root = response.body();
            if (response.httpStatus() < 200 || response.httpStatus() >= 300
                    || root.path("code").asInt(-1) != 0) {
                throw new BuzzException("UniPush 鉴权失败 (code=" + root.path("code").asText("unknown") + ")");
            }
            JsonNode data = root.path("data");
            String token = data.path("token").asText("");
            if (token.isBlank()) {
                throw new BuzzException("UniPush 鉴权响应缺少 token");
            }
            long expiresAt = data.path("expire_time").asLong(now + DEFAULT_TOKEN_TTL_MS);
            cachedToken = new CachedToken(token, Math.max(now, expiresAt - TOKEN_REFRESH_SKEW_MS));
            return token;
        }
    }

    private ApiResponse postJson(String path, String token, Object body) {
        try {
            URI uri = URI.create(baseUrl() + "/" + properties.getAppId() + path);
            HttpRequest.Builder request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(requirePositive(properties.getRequestTimeoutSeconds())))
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8));
            if (token != null) {
                request.header("token", token);
            }
            HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode json = objectMapper.readTree(response.body());
            if (json == null || !json.isObject()) {
                throw new BuzzException("UniPush 返回无效 JSON");
            }
            return new ApiResponse(response.statusCode(), json);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuzzException("UniPush 请求被中断");
        } catch (IOException | IllegalArgumentException e) {
            throw new BuzzException("UniPush 请求失败");
        }
    }

    private String baseUrl() {
        String value = properties.getApiBaseUrl() == null ? "" : properties.getApiBaseUrl().trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private int requirePositive(int value) {
        if (value <= 0) {
            throw new BuzzException("UniPush 请求超时时间必须大于 0");
        }
        return value;
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private boolean isInvalidCidMessage(String message) {
        String normalized = message == null ? "" : message.toLowerCase(java.util.Locale.ROOT);
        return normalized.contains("target user is invalid")
                || normalized.contains("cid is invalid")
                || normalized.contains("appid not match cid");
    }

    private UniPushDeliveryResult failed(String message, boolean invalidCid) {
        return failed(message, invalidCid, "error");
    }

    private UniPushDeliveryResult failed(String message, boolean invalidCid, String providerStatus) {
        return new UniPushDeliveryResult("failed", providerStatus, null, abbreviate(message), invalidCid);
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "UniPush 请求失败";
        }
        return value.length() <= 240 ? value : value.substring(0, 240);
    }

    private static HttpClient createHttpClient(UniPushProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    private record CachedToken(String value, long refreshAt) {
    }

    private record ApiResponse(int httpStatus, JsonNode body) {
    }
}
