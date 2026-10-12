package com.faber.api.base.telemetry.service;

import com.faber.api.base.telemetry.entity.StatEvent;
import com.faber.api.base.telemetry.entity.TelemetryApp;
import com.faber.api.base.telemetry.mapper.StatEventMapper;
import com.faber.api.base.telemetry.vo.TelemetryEventReq;
import org.springframework.stereotype.Service;

/** Stat Event 写入服务；业务模块后续只通过此服务或注解入口产生事件。 */
@Service
public class TelemetryStatService {

    private final StatEventMapper statEventMapper;

    public TelemetryStatService(StatEventMapper statEventMapper) {
        this.statEventMapper = statEventMapper;
    }

    public void persist(TelemetryApp app, TelemetryEventReq request) {
        StatEvent event = new StatEvent();
        event.setAppId(app.getId());
        event.setClientType(request.getClientType());
        event.setEnvironment(request.getEnvironment());
        event.setRelease(request.getRelease());
        event.setSessionId(request.getSessionId());
        event.setUserId(request.getUserId());
        event.setTenantId(request.getTenantId());
        event.setEventType(request.getEventType());
        event.setEventCode(request.getEventCode());
        event.setModule(request.getModule());
        event.setBizType(request.getBizType());
        event.setBizId(request.getBizId());
        event.setResult(request.getResult());
        event.setDuration(request.getDuration());
        event.setProperties(request.getProperties());
        event.setContext(request.getContext());
        event.setOccurTime(request.getOccurTime());
        event.setCreateTime(request.getReceiveTime());
        statEventMapper.insert(event);
    }
}
