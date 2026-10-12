package com.faber.api.base.push.biz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.api.base.push.entity.PushDevice;
import com.faber.api.base.push.mapper.PushDeviceMapper;
import com.faber.api.base.push.unipush.UniPushDeliveryResult;
import com.faber.api.base.push.unipush.UniCloudPushClient;
import com.faber.api.base.push.vo.req.PushTestSendReqVo;
import com.faber.api.base.push.vo.req.PushTestStatusReqVo;
import com.faber.api.base.push.vo.ret.PushTestRunAdminVo;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.auth.UserNoPermissionException;
import com.faber.core.utils.FaRedisUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RAtomicLong;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushTestAdminBizTest {

    @Mock
    private PushDeviceAdminBiz pushDeviceAdminBiz;
    @Mock
    private PushDeviceMapper pushDeviceMapper;
    @Mock
    private UniCloudPushClient uniCloudPushClient;
    @Mock
    private FaRedisUtils faRedisUtils;
    @Mock
    private RedissonClient redisson;
    @Mock
    private RScoredSortedSet<String> recentRuns;
    @Mock
    private RAtomicLong receipt;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks
    private PushTestAdminBiz biz;

    @Test
    void rejectsMoreThanFiveDevicesBeforeLookingUpOrSending() {
        when(uniCloudPushClient.isConfigured()).thenReturn(true);
        PushTestSendReqVo req = request(List.of(1L, 2L, 3L, 4L, 5L, 6L));

        assertThrows(BuzzException.class, () -> biz.send(req));

        verify(pushDeviceMapper, never()).selectList(any());
        verify(uniCloudPushClient, never()).send(any(), any(), any(), any(), anyBoolean());
        verify(faRedisUtils, never()).set(any(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void rejectsRequestWhenAnyDeviceIsMissingBeforeSending() {
        when(uniCloudPushClient.isConfigured()).thenReturn(true);
        when(pushDeviceMapper.selectList(any())).thenReturn(List.of(device(1L)));
        PushTestSendReqVo req = request(List.of(1L, 2L));

        assertThrows(BuzzException.class, () -> biz.send(req));

        verify(uniCloudPushClient, never()).send(any(), any(), any(), any(), anyBoolean());
        verify(faRedisUtils, never()).set(any(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void rejectsDisabledDeviceBeforeSending() {
        PushDevice disabled = device(1L);
        disabled.setEnabled(false);
        when(uniCloudPushClient.isConfigured()).thenReturn(true);
        when(pushDeviceMapper.selectList(any())).thenReturn(List.of(disabled));

        assertThrows(BuzzException.class, () -> biz.send(request(List.of(1L))));

        verify(uniCloudPushClient, never()).send(any(), any(), any(), any(), anyBoolean());
        verify(faRedisUtils, never()).set(any(), any(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void rejectsUnprivilegedUserBeforeReadingOrSending() {
        doThrow(new UserNoPermissionException("无推送设备管理权限"))
                .when(pushDeviceAdminBiz).requireAdminAccess();

        assertThrows(UserNoPermissionException.class, () -> biz.send(request(List.of(1L))));
        assertThrows(UserNoPermissionException.class, () -> biz.recent());
        PushTestStatusReqVo statusReq = new PushTestStatusReqVo();
        statusReq.setTestId("5f94671e-321f-4b91-b2cd-984fe6b80b41");
        assertThrows(UserNoPermissionException.class, () -> biz.status(statusReq));

        verify(pushDeviceMapper, never()).selectList(any());
        verify(uniCloudPushClient, never()).send(any(), any(), any(), any(), anyBoolean());
        verify(faRedisUtils, never()).getStr(any());
    }

    @Test
    void savesPerDeviceProviderResultAndReturnsItFromStatusApi() throws Exception {
        PushDevice device = device(1L);
        ArgumentCaptor<String> payloadValues = ArgumentCaptor.forClass(String.class);
        when(uniCloudPushClient.isConfigured()).thenReturn(true);
        when(uniCloudPushClient.supports(device)).thenReturn(true);
        when(uniCloudPushClient.send(eq(device), eq("标题"), eq("内容"), any(), eq(false)))
                .thenReturn(new UniPushDeliveryResult("accepted", "accepted", "task-1",
                        "UniPush 已受理", false));
        when(pushDeviceMapper.selectList(any())).thenReturn(List.of(device));
        when(faRedisUtils.getRedisson()).thenReturn(redisson);
        when(faRedisUtils.buildKey(any())).thenAnswer(call -> call.getArgument(0));
        org.mockito.Mockito.doReturn(recentRuns).when(redisson).getScoredSortedSet(any(), any());
        when(redisson.getAtomicLong(anyString())).thenReturn(receipt);

        PushTestRunAdminVo sent = biz.send(request(List.of(1L)));

        assertEquals(1, sent.getDevices().size());
        assertEquals("accepted", sent.getDevices().get(0).getStatus());
        assertEquals("accepted", sent.getDevices().get(0).getProviderStatus());
        verify(uniCloudPushClient).send(eq(device), eq("标题"), eq("内容"), payloadValues.capture(), eq(false));

        ArgumentCaptor<String> stateValues = ArgumentCaptor.forClass(String.class);
        String key = "fa:push:test:run:" + sent.getTestId();
        verify(faRedisUtils, times(2)).set(eq(key), stateValues.capture(), eq(24L), eq(TimeUnit.HOURS));
        JsonNode payload = objectMapper.readTree(payloadValues.getValue());
        assertEquals(sent.getTestId(), payload.path("testId").asText());
        assertEquals("/pages/message/index", payload.path("link").asText());

        when(faRedisUtils.getStr(key)).thenReturn(stateValues.getAllValues()
                .get(stateValues.getAllValues().size() - 1));
        PushTestStatusReqVo statusReq = new PushTestStatusReqVo();
        statusReq.setTestId(sent.getTestId());
        PushTestRunAdminVo status = biz.status(statusReq);
        assertEquals(sent.getTestId(), status.getTestId());
        assertEquals("accepted", status.getDevices().get(0).getStatus());
        assertTrue(status.getDevices().get(0).getMessage().contains("已受理"));
    }

    @Test
    void rejectsReservedExtraFieldsThatCouldReplaceTestIdOrLink() throws Exception {
        when(uniCloudPushClient.isConfigured()).thenReturn(true);
        PushTestSendReqVo req = request(List.of(1L));
        req.setExtra(objectMapper.readTree("{\"testId\":\"spoofed\"}"));

        assertThrows(BuzzException.class, () -> biz.send(req));

        verify(pushDeviceMapper, never()).selectList(any());
        verify(uniCloudPushClient, never()).send(any(), any(), any(), any(), anyBoolean());
    }

    private PushTestSendReqVo request(List<Long> deviceIds) {
        PushTestSendReqVo req = new PushTestSendReqVo();
        req.setDeviceIds(deviceIds);
        req.setTitle("标题");
        req.setContent("内容");
        req.setLink("/pages/message/index");
        return req;
    }

    private PushDevice device(Long id) {
        PushDevice device = new PushDevice();
        device.setId(id);
        device.setProvider("unipush");
        device.setClientId("cid-" + id);
        device.setAppId("app-1");
        device.setEnvironment("test");
        device.setEnabled(true);
        return device;
    }
}
