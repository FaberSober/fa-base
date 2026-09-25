package com.faber.api.base.admin.socket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.config.websocket.WsBaseService;
import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.config.websocket.WsClientPresenceStore;
import com.faber.core.annotation.FaWsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@Slf4j
@FaWsService(type = RemoteClientPresenceService.TYPE)
public class RemoteClientPresenceService implements WsBaseService {
    public static final String TYPE = "RemoteClientRegister";

    @Resource
    private WsClientPresenceStore presenceStore;

    @Override
    public void onOpen(WsClientInfoEntity client) {
        presenceStore.refresh(client);
    }

    @Override
    public void onHeartbeat(WsClientInfoEntity client) {
        presenceStore.refresh(client);
    }

    @Override
    public void onMessage(WsClientInfoEntity client, JSONObject message) {
        if (message == null) return;
        String clientType = message.getStr("clientType");
        boolean supported = Arrays.stream(TelemetryClientTypeEnum.values())
                .anyMatch(type -> type.getValue().equals(clientType));
        if (!supported) return;

        client.setClientType(clientType);
        client.setClientInstanceId(read(message, "clientInstanceId", 128));
        client.setRuntime(read(message, "runtime", 40));
        client.setAppCode(read(message, "appCode", 100));
        client.setAppName(read(message, "appName", 100));
        client.setRelease(read(message, "release", 64));
        client.setEnvironment(read(message, "environment", 24));
        client.setPlatform(read(message, "platform", 32));
        client.setOsName(read(message, "osName", 64));
        client.setOsVersion(read(message, "osVersion", 64));
        client.setDeviceModel(read(message, "deviceModel", 100));
        try {
            presenceStore.refresh(client);
        } catch (Exception e) {
            log.error("WebSocket 在线状态写入 Redis 失败 sessionId={}",
                    client.getSession() == null ? null : client.getSession().getId(), e);
        }
    }

    @Override
    public void onClose(WsClientInfoEntity client) {
        if (client.getSession() != null) {
            presenceStore.remove(client.getSession().getId());
        }
    }

    private String read(JSONObject message, String key, int maxLength) {
        String value = message.getStr(key);
        if (StrUtil.isBlank(value)) return null;
        value = value.trim();
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
