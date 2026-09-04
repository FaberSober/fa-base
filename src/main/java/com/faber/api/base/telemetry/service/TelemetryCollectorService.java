package com.faber.api.base.telemetry.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.api.base.telemetry.biz.TelemetryAppBiz;
import com.faber.api.base.telemetry.entity.TelemetryApp;
import com.faber.api.base.telemetry.vo.TelemetryBaseReq;
import com.faber.api.base.telemetry.vo.TelemetryErrorReq;
import com.faber.api.base.telemetry.vo.TelemetryEventReq;
import com.faber.core.exception.BuzzException;
import org.springframework.stereotype.Service;

import java.util.Date;

/** Collector 的公共校验与接收时间补充。 */
@Service
public class TelemetryCollectorService {

    private static final int MAX_CONTEXT_BYTES = 16 * 1024;
    private static final int MAX_PROPERTIES_BYTES = 16 * 1024;
    private static final int MAX_BREADCRUMBS_BYTES = 16 * 1024;

    private final TelemetryAppBiz telemetryAppBiz;
    private final ObjectMapper objectMapper;

    public TelemetryCollectorService(TelemetryAppBiz telemetryAppBiz, ObjectMapper objectMapper) {
        this.telemetryAppBiz = telemetryAppBiz;
        this.objectMapper = objectMapper;
    }

    public void acceptError(TelemetryErrorReq request) {
        validateBase(request);
        validateJsonSize(request.getBreadcrumbs(), MAX_BREADCRUMBS_BYTES, "Breadcrumb");
    }

    public void acceptEvent(TelemetryEventReq request) {
        validateBase(request);
        validateJsonSize(request.getProperties(), MAX_PROPERTIES_BYTES, "Properties");
    }

    private void validateBase(TelemetryBaseReq request) {
        TelemetryApp app = telemetryAppBiz.findEnabledByAppKey(request.getAppKey());
        if (app == null) {
            throw new BuzzException("Telemetry AppKey 无效或应用已停用");
        }
        if (app.getClientType() != request.getClientType()) {
            throw new BuzzException("Telemetry Client Type 与应用配置不一致");
        }
        validateJsonSize(request.getContext(), MAX_CONTEXT_BYTES, "Context");
        Date receiveTime = new Date();
        request.setReceiveTime(receiveTime);
        if (request.getOccurTime() == null) {
            request.setOccurTime(receiveTime);
        }
    }

    private void validateJsonSize(JsonNode value, int maxBytes, String fieldName) {
        if (value == null || value.isNull()) {
            return;
        }
        try {
            if (objectMapper.writeValueAsBytes(value).length > maxBytes) {
                throw new BuzzException(fieldName + " 超过 " + maxBytes + " 字节限制");
            }
        } catch (JsonProcessingException e) {
            throw new BuzzException(fieldName + " 格式无效");
        }
    }
}
