package com.faber.api.base.push.biz;

import com.faber.api.base.push.entity.PushDevice;
import com.faber.api.base.push.mapper.PushDeviceMapper;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;

/**
 * BASE-推送设备
 */
@Service
public class PushDeviceBiz extends BaseBiz<PushDeviceMapper, PushDevice> {
}
