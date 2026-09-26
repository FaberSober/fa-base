package com.faber.api.base.push.unipush;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.api.base.push.entity.PushDevice;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UniPushRestClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void authenticatesOnceAndSendsPerCidNotificationWithPayload() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> auth = response(200, "{\"code\":0,\"data\":{\"token\":\"provider-token\",\"expire_time\":9999999999999}} ");
        HttpResponse<String> push = response(200,
                "{\"code\":0,\"data\":{\"task-123\":{\"cid-1\":\"successed_online\"}}}");
        doReturn(auth).doReturn(push).doReturn(push)
                .when(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        UniPushRestClient client = new UniPushRestClient(properties(), objectMapper, httpClient);
        PushDevice device = device("cid-1");
        String payload = "{\"type\":\"adminPushTest\",\"testId\":\"test-id\",\"link\":\"/pages/message/index\"}";

        UniPushDeliveryResult first = client.send(device, "标题", "内容", payload);
        UniPushDeliveryResult second = client.send(device, "标题2", "内容2", payload);

        assertEquals("accepted", first.status());
        assertEquals("successed_online", first.providerStatus());
        assertEquals("task-123", first.providerTaskId());
        assertEquals("accepted", second.status());
        verify(httpClient, times(3)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        var requests = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient, times(3)).send(requests.capture(), any(HttpResponse.BodyHandler.class));
        List<HttpRequest> captured = requests.getAllValues();
        assertEquals("/v2/test-app/auth", captured.get(0).uri().getPath());
        assertEquals("/v2/test-app/push/single/cid", captured.get(1).uri().getPath());
        assertEquals("provider-token", captured.get(1).headers().firstValue("token").orElseThrow());

        JsonNode requestBody = objectMapper.readTree(readBody(captured.get(1)));
        assertEquals(32, requestBody.path("request_id").asText().length());
        assertEquals("cid-1", requestBody.path("audience").path("cid").get(0).asText());
        JsonNode notification = requestBody.path("push_message").path("notification");
        assertEquals("payload", notification.path("click_type").asText());
        assertEquals(payload, notification.path("payload").asText());
        assertEquals("标题", requestBody.path("push_channel").path("ios").path("aps")
                .path("alert").path("title").asText());
    }

    @Test
    void reportsInvalidCidWithoutLeakingProviderToken() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> auth = response(200,
                "{\"code\":0,\"data\":{\"token\":\"provider-token\",\"expire_time\":9999999999999}}");
        HttpResponse<String> invalidCid = response(400,
                "{\"code\":20001,\"msg\":\"target user is invalid\"}");
        doReturn(auth).doReturn(invalidCid)
                .when(httpClient).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));

        UniPushRestClient client = new UniPushRestClient(properties(), objectMapper, httpClient);
        UniPushDeliveryResult result = client.send(device("bad-cid"), "标题", "内容", "{}");

        assertEquals("failed", result.status());
        assertTrue(result.invalidClientId());
        assertFalse(result.message().contains("provider-token"));
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
        properties.setApiBaseUrl("https://restapi.getui.com/v2");
        properties.setAppId("test-app");
        properties.setAppKey("test-key");
        properties.setMasterSecret("test-secret");
        return properties;
    }

    private PushDevice device(String clientId) {
        PushDevice device = new PushDevice();
        device.setId(1L);
        device.setClientId(clientId);
        device.setAppId("test-app");
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
