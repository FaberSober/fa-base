package com.faber.api.base.telemetry.service;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.api.base.telemetry.vo.TelemetryEventReq;
import com.faber.api.base.telemetry.vo.TelemetryTrackCommand;
import com.faber.core.context.BaseContextHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

/** 服务端业务统计门面，失败或缺少客户端上下文时均不影响业务请求。 */
@Slf4j
@Service
public class TelemetryService {

    public static final String HEADER_APP_KEY = "X-Telemetry-App-Key";
    public static final String HEADER_CLIENT_TYPE = "X-Telemetry-Client-Type";
    public static final String HEADER_ENVIRONMENT = "X-Telemetry-Environment";
    public static final String HEADER_RELEASE = "X-Telemetry-Release";
    public static final String HEADER_SESSION_ID = "X-Telemetry-Session-Id";

    private final TelemetryCollectorService collectorService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public TelemetryService(TelemetryCollectorService collectorService, ObjectMapper objectMapper, Validator validator) {
        this.collectorService = collectorService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public void track(TelemetryTrackCommand command) {
        try {
            TelemetryEventReq request = buildRequest(command);
            if (request == null) {
                return;
            }
            Set<ConstraintViolation<TelemetryEventReq>> violations = validator.validate(request);
            if (!violations.isEmpty()) {
                log.warn("Telemetry 服务端事件参数无效，eventCode：{}", command.getEventCode());
                return;
            }
            collectorService.acceptEvent(request);
        } catch (Exception e) {
            log.warn("Telemetry 服务端事件上报失败，eventCode：{}", command.getEventCode(), e);
        }
    }

    private TelemetryEventReq buildRequest(TelemetryTrackCommand command) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest servletRequest = attributes.getRequest();
        String appKey = servletRequest.getHeader(HEADER_APP_KEY);
        String clientType = servletRequest.getHeader(HEADER_CLIENT_TYPE);
        String environment = servletRequest.getHeader(HEADER_ENVIRONMENT);
        String release = servletRequest.getHeader(HEADER_RELEASE);
        String sessionId = servletRequest.getHeader(HEADER_SESSION_ID);
        if (StrUtil.hasBlank(appKey, clientType, environment, release, sessionId)) {
            return null;
        }

        TelemetryEventReq request = new TelemetryEventReq();
        request.setAppKey(appKey);
        request.setClientType(TelemetryClientTypeEnum.valueOf(clientType));
        request.setEnvironment(environment);
        request.setRelease(release);
        request.setSessionId(sessionId);
        request.setUserId(BaseContextHandler.getUserId());
        request.setTenantId(BaseContextHandler.getTenantId());
        request.setEventType(command.getEventType());
        request.setEventCode(command.getEventCode());
        request.setModule(command.getModule());
        request.setBizType(command.getBizType());
        request.setBizId(command.getBizId());
        request.setResult(command.getResult());
        request.setDuration(command.getDuration());
        request.setProperties(command.getProperties());
        ObjectNode context = objectMapper.createObjectNode();
        context.put("requestPath", servletRequest.getRequestURI());
        request.setContext(context);
        return request;
    }
}
