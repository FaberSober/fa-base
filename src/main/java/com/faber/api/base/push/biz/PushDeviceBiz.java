package com.faber.api.base.push.biz;

import com.faber.api.base.push.entity.PushDevice;
import com.faber.api.base.push.mapper.PushDeviceMapper;
import com.faber.api.portal.push.vo.PortalPushDeviceBindingReqVo;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Locale;

/**
 * BASE-推送设备
 */
@Service
public class PushDeviceBiz extends BaseBiz<PushDeviceMapper, PushDevice> {

    /** Register or refresh the current user's push-device binding. */
    @Transactional(rollbackFor = Exception.class)
    public void registerCurrentUser(PortalPushDeviceBindingReqVo reqVo) {
        String userId = getCurrentUserId();
        String provider = normalizeProvider(reqVo.getProvider());
        String clientId = reqVo.getClientId().trim();
        String appId = reqVo.getAppId().trim();
        String environment = reqVo.getEnvironment().trim().toLowerCase(Locale.ROOT);
        String platform = reqVo.getPlatform().trim().toLowerCase(Locale.ROOT);

        String previousClientId = reqVo.getPreviousClientId();
        if (previousClientId != null && !previousClientId.isBlank()
                && !previousClientId.trim().equals(clientId)) {
            lambdaUpdate()
                    .eq(PushDevice::getUserId, userId)
                    .eq(PushDevice::getProvider, provider)
                    .eq(PushDevice::getClientId, previousClientId.trim())
                    .eq(PushDevice::getAppId, appId)
                    .eq(PushDevice::getEnvironment, environment)
                    .set(PushDevice::getEnabled, false)
                    .update();
        }

        PushDevice device = lambdaQuery()
                .eq(PushDevice::getProvider, provider)
                .eq(PushDevice::getClientId, clientId)
                .eq(PushDevice::getAppId, appId)
                .eq(PushDevice::getEnvironment, environment)
                .one();

        Date now = new Date();
        if (device == null) {
            device = new PushDevice();
            device.setUserId(userId);
            device.setProvider(provider);
            device.setClientId(clientId);
            device.setAppId(appId);
            device.setPlatform(platform);
            device.setEnvironment(environment);
            device.setEnabled(true);
            device.setLastSeenTime(now);
            save(device);
            return;
        }

        // A CID is globally unique per provider/app/environment; a fresh login rebinds it to the current user.
        device.setUserId(userId);
        device.setPlatform(platform);
        device.setEnabled(true);
        device.setLastSeenTime(now);
        device.setInvalidTime(null);
        updateById(device);
    }

    /** Disable only the current user's matching binding; repeated calls are safe. */
    @Transactional(rollbackFor = Exception.class)
    public void unregisterCurrentUser(PortalPushDeviceBindingReqVo reqVo) {
        lambdaUpdate()
                .eq(PushDevice::getUserId, getCurrentUserId())
                .eq(PushDevice::getProvider, normalizeProvider(reqVo.getProvider()))
                .eq(PushDevice::getClientId, reqVo.getClientId().trim())
                .eq(PushDevice::getAppId, reqVo.getAppId().trim())
                .eq(PushDevice::getEnvironment, reqVo.getEnvironment().trim().toLowerCase(Locale.ROOT))
                .set(PushDevice::getEnabled, false)
                .update();
    }

    private String normalizeProvider(String provider) {
        String normalized = provider.trim().toLowerCase(Locale.ROOT);
        if (!"unipush".equals(normalized)) {
            throw new BuzzException("不支持的推送服务商");
        }
        return normalized;
    }
}
