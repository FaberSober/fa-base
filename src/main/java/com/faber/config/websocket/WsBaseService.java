package com.faber.config.websocket;

import cn.hutool.json.JSONObject;

public interface WsBaseService {

    void onMessage(WsClientInfoEntity entity, JSONObject msg);

    default void onOpen(WsClientInfoEntity entity) {
    }

    default void onHeartbeat(WsClientInfoEntity entity) {
    }

    default void onClose(WsClientInfoEntity entity) {
    }

}
