package com.faber.api.base.telemetry.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.faber.api.base.telemetry.entity.TelemetryApp;
import com.faber.api.base.telemetry.mapper.TelemetryAppMapper;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;

/** Telemetry 应用管理。 */
@Service
public class TelemetryAppBiz extends BaseBiz<TelemetryAppMapper, TelemetryApp> {

    public TelemetryApp findEnabledByAppKey(String appKey) {
        return getOne(new LambdaQueryWrapper<TelemetryApp>()
                .eq(TelemetryApp::getAppKey, appKey)
                .eq(TelemetryApp::getEnabled, true));
    }
}
