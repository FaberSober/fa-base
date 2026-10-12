package com.faber.api.base.admin.socket;

import cn.hutool.json.JSONObject;
import com.faber.config.websocket.WsBaseService;
import com.faber.config.websocket.WsClientInfoEntity;
import com.faber.core.annotation.FaWsService;
import com.faber.core.service.SocketTaskProgressService;
import com.faber.core.vo.socket.SocketTaskVo;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@FaWsService(type = WebSocketTaskImpl.TYPE)
public class WebSocketTaskImpl implements WsBaseService, SocketTaskProgressService {
    public static final String TYPE = "WebSocketTaskDemo";

    private static final Map<String, Map<String, WsClientInfoEntity>> taskSessions = new ConcurrentHashMap<>();

    @Override
    public void onMessage(WsClientInfoEntity entity, JSONObject msg) {
        String taskId = msg.getStr("taskId");
        String sessionId = entity.getSession().getId();
        taskSessions.compute(taskId, (key, sessions) -> {
            if (sessions == null) sessions = new ConcurrentHashMap<>();
            sessions.put(sessionId, entity);
            return sessions;
        });
    }

    @Override
    public void onClose(WsClientInfoEntity entity) {
        String sessionId = entity.getSession().getId();
        taskSessions.forEach((taskId, sessions) -> taskSessions.computeIfPresent(taskId, (key, current) -> {
            current.remove(sessionId, entity);
            return current.isEmpty() ? null : current;
        }));
    }

    private static void doSendProgress(SocketTaskVo socketTaskVo) {
        Map<String, WsClientInfoEntity> sessions = taskSessions.get(socketTaskVo.getTaskId());
        if (sessions == null) return;
        sessions.values().forEach(entity -> entity.sendMessage(TYPE, socketTaskVo));
    }

    public static void sendProgress(SocketTaskVo socketTaskVo) {
        doSendProgress(socketTaskVo);
    }

    @Override
    public void sendTaskProgress(SocketTaskVo socketTaskVo) {
        doSendProgress(socketTaskVo);
    }

}
