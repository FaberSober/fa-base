package com.faber.api.base.push.unipush;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.api.base.push.entity.PushDevice;
import com.faber.core.exception.BuzzException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.net.ssl.SSLException;

/** Calls the URLized UniCloud function that sends messages through DCloud UniPush 2.0. */
@Service
public class UniCloudPushClient {

    private static final String SIGNATURE_HEADER = "X-Fa-Push-Signature";
    private static final String TIMESTAMP_HEADER = "X-Fa-Push-Timestamp";
    private static final String NONCE_HEADER = "X-Fa-Push-Nonce";

    private final UniPushProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    @Autowired
    public UniCloudPushClient(UniPushProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, createHttpClient(properties));
    }

    UniCloudPushClient(UniPushProperties properties, ObjectMapper objectMapper, HttpClient httpClient) {
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
        return send(device, title, body, payloadJson, false);
    }

    public UniPushDeliveryResult send(PushDevice device, String title, String body, String payloadJson,
                                      boolean forceNotification) {
        if (!isConfigured()) {
            throw new BuzzException("UniPush 2.0 尚未配置 AppID、UniCloud 云函数地址和调用密钥");
        }
        if (device == null || device.getClientId() == null || device.getClientId().isBlank()) {
            throw new BuzzException("推送设备缺少 Client ID");
        }

        JsonNode payload;
        try {
            payload = objectMapper.readTree(payloadJson == null || payloadJson.isBlank() ? "{}" : payloadJson);
        } catch (JsonProcessingException e) {
            throw new BuzzException("推送扩展数据格式无效");
        }
        if (payload == null || !payload.isObject()) {
            throw new BuzzException("推送扩展数据必须是 JSON 对象");
        }

        Map<String, Object> bodyMap = new LinkedHashMap<>();
        bodyMap.put("appId", properties.getClientAppId());
        bodyMap.put("pushClientId", device.getClientId());
        bodyMap.put("title", title);
        bodyMap.put("content", body);
        bodyMap.put("forceNotification", forceNotification);
        bodyMap.put("payload", payload);

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(bodyMap);
        } catch (JsonProcessingException e) {
            throw new BuzzException("UniCloud 推送请求序列化失败");
        }

        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString();
        String signature = sign(timestamp, nonce, requestBody);
        HttpResponse<String> response = post(requestBody, timestamp, nonce, signature);
        JsonNode result = parseResponse(response);

        boolean success = response.statusCode() >= 200 && response.statusCode() < 300
                && result.path("success").asBoolean(false);
        String providerStatus = text(result, "providerStatus", success ? "accepted" : "request_failed");
        String providerTaskId = textOrNull(result, "providerTaskId");
        boolean invalidClientId = result.path("invalidClientId").asBoolean(false);
        if (!success) {
            String message = text(result, "message", "UniCloud 推送云函数调用失败");
            return failed(message, invalidClientId, providerStatus);
        }
        return new UniPushDeliveryResult("accepted", providerStatus, providerTaskId,
                "UniPush 2.0 已受理，设备是否收到需等待客户端回执", false);
    }

    private HttpResponse<String> post(String requestBody, String timestamp, String nonce, String signature) {
        HttpResponse<String> response;
        try {
            HttpRequest request = HttpRequest.newBuilder(functionUri())
                    .timeout(Duration.ofSeconds(requirePositive(properties.getRequestTimeoutSeconds())))
                    .header("Content-Type", "application/json;charset=UTF-8")
                    .header(TIMESTAMP_HEADER, timestamp)
                    .header(NONCE_HEADER, nonce)
                    .header(SIGNATURE_HEADER, signature)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException e) {
            throw new BuzzException(exceptionMessage("UniCloud 推送请求超时", e));
        } catch (UnknownHostException e) {
            throw new BuzzException(exceptionMessage("UniCloud 云函数域名解析失败", e));
        } catch (ConnectException e) {
            throw new BuzzException(exceptionMessage("无法连接 UniCloud 云函数", e));
        } catch (SSLException e) {
            throw new BuzzException(exceptionMessage("UniCloud 云函数 TLS 连接失败", e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuzzException("UniCloud 推送请求被中断");
        } catch (IOException e) {
            throw new BuzzException(exceptionMessage("UniCloud 推送网络请求失败", e));
        } catch (IllegalArgumentException e) {
            throw new BuzzException(exceptionMessage("UniCloud 云函数地址或配置参数无效", e));
        }
        return response;
    }

    private URI functionUri() {
        URI uri;
        try {
            uri = URI.create(properties.getCloudFunctionUrl().trim());
        } catch (RuntimeException e) {
            throw new BuzzException("UniCloud 云函数地址无效");
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        boolean localHttp = "http".equalsIgnoreCase(scheme)
                && ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host));
        if (host == null || !("https".equalsIgnoreCase(scheme) || localHttp)) {
            throw new BuzzException("UniCloud 云函数地址必须使用 HTTPS");
        }
        return uri;
    }

    private JsonNode parseResponse(HttpResponse<String> response) {
        try {
            JsonNode json = objectMapper.readTree(response.body());
            if (json == null || !json.isObject()) {
                throw new BuzzException("UniCloud 云函数返回无效 JSON (HTTP " + response.statusCode() + ")");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new BuzzException("UniCloud 云函数返回内容不是有效 JSON (HTTP " + response.statusCode() + ")");
        }
    }

    private String sign(String timestamp, String nonce, String body) {
        String canonical = timestamp + "\n" + nonce + "\n" + body;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getCloudFunctionSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is unavailable", e);
        }
    }

    private int requirePositive(int value) {
        if (value <= 0) {
            throw new BuzzException("UniPush 请求超时时间必须大于 0");
        }
        return value;
    }

    private String exceptionMessage(String prefix, Exception exception) {
        String detail = safeText(exception.getMessage());
        String type = exception.getClass().getSimpleName();
        return prefix + " (" + type + (detail.isBlank() ? ")" : ": " + detail + ")");
    }

    private String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() ? fallback : safeText(value);
    }

    private String textOrNull(JsonNode node, String field) {
        String value = node.path(field).asText("");
        return value.isBlank() ? null : safeText(value);
    }

    private UniPushDeliveryResult failed(String message, boolean invalidClientId, String providerStatus) {
        String safeMessage = safeText(message);
        if (safeMessage.isBlank()) {
            safeMessage = "UniCloud 推送云函数调用失败";
        }
        return new UniPushDeliveryResult("failed", safeText(providerStatus), null,
                safeMessage.length() <= 240 ? safeMessage : safeMessage.substring(0, 240), invalidClientId);
    }

    private String safeText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String safeValue = value.replaceAll("[\\r\\n\\t]+", " ").trim();
        String secret = properties.getCloudFunctionSecret();
        if (secret != null && !secret.isBlank()) {
            safeValue = safeValue.replace(secret, "[已隐藏]");
        }
        return safeValue;
    }

    private static HttpClient createHttpClient(UniPushProperties properties) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }
}
