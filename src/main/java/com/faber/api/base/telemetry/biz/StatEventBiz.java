package com.faber.api.base.telemetry.biz;

import com.faber.api.base.telemetry.entity.StatEvent;
import com.faber.api.base.telemetry.mapper.StatEventMapper;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;

/** Telemetry 业务事件明细查询。 */
@Service
public class StatEventBiz extends BaseBiz<StatEventMapper, StatEvent> {
}
