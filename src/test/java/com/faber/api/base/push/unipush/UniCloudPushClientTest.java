package com.faber.api.base.push.unipush;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.api.base.push.entity.PushDevice;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UniCloudPushClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void callsSignedUniCloudFunctionWithAppIdAndPayload() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = response(200,
                "{\"success\":true,\"providerStatus\":\"accepted\",\"providerTaskId\":\"task-123\"}");
        doReturn(response, response).when(httpClient)
                .send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());

        UniCloudPushClient client = new UniCloudPushClient(properties(), objectMapper, httpClient);
        PushDevice device = device("cid-1");
        String payload = "{\"type\":\"adminPushTest\",\"testId\":\"test-id\",\"link\":\"/pages/message/index\"}";

        UniPushDeliveryResult first = client.send(device, "标题", "内容", payload);
        UniPushDeliveryResult second = client.send(device, "标题2", "内容2", payload);

        assertEquals("accepted", first.status());
        assertEquals("accepted", first.providerStatus());
        assertEquals("task-123", first.providerTaskId());
        assertEquals("accepted", second.status());
        verify(httpClient, times(2)).send(any(HttpRequest.class),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());

        var requests = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(2)).send(requests.capture(),
                org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
        List<HttpRequest> captured = requests.getAllValues();
        assertEquals("https://functions.example.test/fa-unipush-send", captured.get(0).uri().toString());
        assertTrue(captured.get(0).headers().firstValue("X-Fa-Push-Timestamp").isPresent());
        assertTrue(captured.get(0).headers().firstValue("X-Fa-Push-Nonce").isPresent());
        String signature = captured.get(0).headers().firstValue("X-Fa-Push-Signature").orElseThrow();
        assertTrue(signature.matches("[a-f0-9]{64}"));

        String requestJson = readBody(captured.get(0));
        JsonNode requestBody = objectMapper.readTree(requestJson);
        assertEquals("__UNI__TEST", requestBody.path("appId").asText());
        assertEquals("cid-1", requestBody.path("pushClientId").asText());
        assertEquals("标题", requestBody.path("title").asText());
        assertEquals("内容", requestBody.path("content").asText());
        assertEquals("test-id", requestBody.path("payload").path("testId").asText());
        assertFalse(requestJson.contains("masterSecret"));
    }

    @Test
    void reportsInvalidCidWithoutLeakingCloudFunctionSecret() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        doReturn(response(200,
                "{\"success\":false,\"providerStatus\":\"invalid_cid\",\"message\":\"invalid cid\",\"invalidClientId\":true}"))
                .when(httpClient).send(any(HttpRequest.class),
                        org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());

        UniCloudPushClient client = new UniCloudPushClient(properties(), objectMapper, httpClient);
        UniPushDeliveryResult result = client.send(device("bad-cid"), "标题", "内容", "{}");

        assertEquals("failed", result.status());
        assertTrue(result.invalidClientId());
        assertFalse(result.message().contains("cloud-test-secret"));
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int status, String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        return response;
    }

    private UniPushProperties properties() {
        UniPushProperties properties = new UniPushProperties();
        properties.setClientAppId("__UNI__TEST");
        properties.setCloudFunctionUrl("https://functions.example.test/fa-unipush-send");
        properties.setCloudFunctionSecret("cloud-test-secret");
        return properties;
    }

    private PushDevice device(String clientId) {
        PushDevice device = new PushDevice();
        device.setId(1L);
        device.setClientId(clientId);
        device.setAppId("__UNI__TEST");
        device.setEnvironment("test");
        device.setProvider("unipush");
        device.setEnabled(true);
        return device;
    }

    private String readBody(HttpRequest request) throws Exception {
        CompletableFuture<String> body = new CompletableFuture<>();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                output.write(bytes, 0, bytes.length);
            }

            @Override
            public void onError(Throwable throwable) {
                body.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                body.complete(output.toString(StandardCharsets.UTF_8));
            }
        });
        return body.get(1, TimeUnit.SECONDS);
    }
}
