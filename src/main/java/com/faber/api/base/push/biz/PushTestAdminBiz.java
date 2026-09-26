package com.faber.api.base.push.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.faber.api.base.push.entity.PushDevice;
import com.faber.api.base.push.mapper.PushDeviceMapper;
import com.faber.api.base.push.unipush.UniPushDeliveryResult;
import com.faber.api.base.push.unipush.UniCloudPushClient;
import com.faber.api.base.push.vo.req.PushTestSendReqVo;
import com.faber.api.base.push.vo.req.PushTestStatusReqVo;
import com.faber.api.base.push.vo.ret.PushTestRunAdminVo;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaRedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class PushTestAdminBiz {

    private static final int MAX_DEVICES = 5;
    private static final int MAX_PAYLOAD_LENGTH = 3072;
    private static final long RUN_TTL_HOURS = 24;
    private static final Set<String> RESERVED_EXTRA_KEYS = Set.of("type", "testId", "link", "route");
    private static final String RUN_KEY_PREFIX = "fa:push:test:run:";

    @Resource
    private PushDeviceAdminBiz pushDeviceAdminBiz;
    @Resource
    private PushDeviceMapper pushDeviceMapper;
    @Resource
    private UniCloudPushClient uniCloudPushClient;
    @Resource
    private FaRedisUtils faRedisUtils;
    @Resource
    private ObjectMapper objectMapper;

    public PushTestRunAdminVo send(PushTestSendReqVo reqVo) {
        pushDeviceAdminBiz.requireAdminAccess();
        if (!uniCloudPushClient.isConfigured()) {
            throw new BuzzException("UniPush 2.0 尚未配置 AppID、UniCloud 云函数地址和调用密钥");
        }

        List<Long> deviceIds = validateAndNormalize(reqVo);
        List<PushDevice> devices = loadAndValidateDevices(deviceIds);
        String testId = UUID.randomUUID().toString();
        String payload = buildPayload(testId, reqVo);

        PushTestRunAdminVo run = new PushTestRunAdminVo();
        run.setTestId(testId);
        run.setCreatedAt(System.currentTimeMillis());
        for (Long deviceId : deviceIds) {
            PushTestRunAdminVo.DeviceResult result = new PushTestRunAdminVo.DeviceResult();
            result.setDeviceId(deviceId);
            result.setStatus("pending");
            result.setUpdatedAt(run.getCreatedAt());
            run.getDevices().add(result);
        }
        saveRun(run);

        Map<Long, PushDevice> devicesById = new HashMap<>();
        devices.forEach(device -> devicesById.put(device.getId(), device));
        for (PushTestRunAdminVo.DeviceResult result : run.getDevices()) {
            PushDevice device = devicesById.get(result.getDeviceId());
            try {
                UniPushDeliveryResult delivery = uniCloudPushClient.send(
                        device, reqVo.getTitle().trim(), reqVo.getContent().trim(), payload,
                        Boolean.TRUE.equals(reqVo.getForceNotification()));
                result.setStatus(delivery.status());
                result.setProviderStatus(delivery.providerStatus());
                result.setProviderTaskId(delivery.providerTaskId());
                result.setMessage(delivery.message());
                if (delivery.invalidClientId()) {
                    disableInvalidDevice(device);
                }
            } catch (RuntimeException e) {
                log.warn("UniPush 2.0 send failed for testId={}, deviceId={}, errorType={}",
                        testId, device.getId(), e.getClass().getSimpleName());
                result.setStatus("failed");
                result.setProviderStatus("request_failed");
                result.setMessage(getSafeFailureMessage(e));
            }
            result.setUpdatedAt(System.currentTimeMillis());
            saveRun(run);
        }
        return run;
    }

    public PushTestRunAdminVo status(PushTestStatusReqVo reqVo) {
        pushDeviceAdminBiz.requireAdminAccess();
        if (reqVo == null || reqVo.getTestId() == null) {
            throw new BuzzException("测试编号不能为空");
        }
        String testId = reqVo.getTestId().trim();
        try {
            if (!UUID.fromString(testId).toString().equalsIgnoreCase(testId)) {
                throw new IllegalArgumentException("non-canonical UUID");
            }
        } catch (IllegalArgumentException e) {
            throw new BuzzException("测试编号格式无效");
        }
        String value = faRedisUtils.getStr(runKey(testId));
        if (value == null || value.isBlank()) {
            throw new BuzzException("测试记录不存在或已超过 24 小时");
        }
        try {
            return objectMapper.readValue(value, PushTestRunAdminVo.class);
        } catch (JsonProcessingException e) {
            throw new BuzzException("测试记录读取失败");
        }
    }

    private List<Long> validateAndNormalize(PushTestSendReqVo reqVo) {
        if (reqVo == null || reqVo.getDeviceIds() == null || reqVo.getDeviceIds().isEmpty()) {
            throw new BuzzException("请至少选择一台推送设备");
        }
        if (reqVo.getDeviceIds().size() > MAX_DEVICES) {
            throw new BuzzException("单次最多选择 5 台设备");
        }
        Set<Long> uniqueIds = new HashSet<>();
        for (Long id : reqVo.getDeviceIds()) {
            if (id == null || id <= 0 || !uniqueIds.add(id)) {
                throw new BuzzException("推送设备编号无效或重复");
            }
        }
        requireText(reqVo.getTitle(), 50, "标题");
        requireText(reqVo.getContent(), 256, "内容");
        if (reqVo.getLink() != null && reqVo.getLink().trim().length() > 1024) {
            throw new BuzzException("应用内链接不能超过 1024 个字符");
        }
        String link = reqVo.getLink() == null ? "" : reqVo.getLink().trim();
        String lowerLink = link.toLowerCase(java.util.Locale.ROOT);
        if (link.startsWith("//") || lowerLink.contains("://")
                || lowerLink.startsWith("javascript:") || lowerLink.startsWith("data:")) {
            throw new BuzzException("链接必须使用应用内路由，不能填写外部 URL");
        }
        JsonNode extra = reqVo.getExtra();
        if (extra != null && !extra.isNull() && !extra.isObject()) {
            throw new BuzzException("扩展 JSON 必须是对象");
        }
        if (extra != null && extra.isObject()) {
            for (String key : RESERVED_EXTRA_KEYS) {
                if (extra.has(key)) {
                    throw new BuzzException("扩展 JSON 不能覆盖保留字段: " + key);
                }
            }
        }
        return new ArrayList<>(reqVo.getDeviceIds());
    }

    private List<PushDevice> loadAndValidateDevices(List<Long> deviceIds) {
        List<PushDevice> devices = pushDeviceMapper.selectList(new LambdaQueryWrapper<PushDevice>()
                .in(PushDevice::getId, deviceIds)
                .eq(PushDevice::getProvider, "unipush"));
        Map<Long, PushDevice> devicesById = new HashMap<>();
        devices.forEach(device -> devicesById.put(device.getId(), device));
        if (devicesById.size() != deviceIds.size()) {
            throw new BuzzException("所选设备不存在或不支持 UniPush");
        }

        for (Long deviceId : deviceIds) {
            PushDevice device = devicesById.get(deviceId);
            if (!Boolean.TRUE.equals(device.getEnabled()) || device.getInvalidTime() != null
                    || device.getClientId() == null || device.getClientId().isBlank()) {
                throw new BuzzException("所选设备已停用或无效，请刷新设备列表");
            }
            if (!uniCloudPushClient.supports(device)) {
                throw new BuzzException("所选设备的 App 或环境未被 UniPush 2.0 配置允许");
            }
        }
        return devices;
    }

    private String buildPayload(String testId, PushTestSendReqVo reqVo) {
        ObjectNode payload = objectMapper.createObjectNode();
        JsonNode extra = reqVo.getExtra();
        if (extra != null && extra.isObject()) {
            extra.fields().forEachRemaining(field -> payload.set(field.getKey(), field.getValue().deepCopy()));
        }
        payload.put("type", "adminPushTest");
        payload.put("testId", testId);
        payload.put("link", reqVo.getLink() == null ? "" : reqVo.getLink().trim());
        try {
            String json = objectMapper.writeValueAsString(payload);
            if (json.length() > MAX_PAYLOAD_LENGTH) {
                throw new BuzzException("测试推送扩展数据不能超过 3072 个字符");
            }
            return json;
        } catch (JsonProcessingException e) {
            throw new BuzzException("测试推送扩展数据格式无效");
        }
    }

    private void requireText(String value, int maxLength, String fieldName) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new BuzzException(fieldName + "不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new BuzzException(fieldName + "不能超过 " + maxLength + " 个字符");
        }
    }

    private String getSafeFailureMessage(RuntimeException error) {
        if (error instanceof BuzzException && error.getMessage() != null && !error.getMessage().isBlank()) {
            String message = error.getMessage().replaceAll("[\\r\\n\\t]+", " ").trim();
            return message.length() <= 240 ? message : message.substring(0, 240);
        }
        return "UniPush 服务请求失败（" + error.getClass().getSimpleName() + "）";
    }

    private void disableInvalidDevice(PushDevice device) {
        device.setEnabled(false);
        device.setInvalidTime(new Date());
        try {
            pushDeviceMapper.updateById(device);
        } catch (RuntimeException e) {
            log.warn("Failed to disable invalid push device, deviceId={}, errorType={}",
                    device.getId(), e.getClass().getSimpleName());
        }
    }

    private void saveRun(PushTestRunAdminVo run) {
        try {
            faRedisUtils.set(runKey(run.getTestId()), objectMapper.writeValueAsString(run),
                    RUN_TTL_HOURS, TimeUnit.HOURS);
        } catch (JsonProcessingException e) {
            throw new BuzzException("测试状态暂时无法保存");
        }
    }

    private String runKey(String testId) {
        return RUN_KEY_PREFIX + testId;
    }
}
