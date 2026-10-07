package com.faber.api.base.admin.biz;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.entity.UserDevice;
import com.faber.api.base.admin.mapper.UserDeviceMapper;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.api.portal.auth.vo.PortalLoginDeviceRetVo;
import com.faber.core.constant.FaSetting;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Date;
import java.util.List;

/**
 * BASE-用户设备
 *
 * @author xu.pengfei
 * @email faberxu@gmail.com
 * @date 2024-01-11 14:52:44
 */
@Service
public class UserDeviceBiz extends BaseBiz<UserDeviceMapper,UserDevice> {

    private static final SecureRandom TRUST_RANDOM = new SecureRandom();

    @Resource
    private FaSetting faSetting;

    @Resource
    UserBiz userBiz;

    @Override
    public void decorateOne(UserDevice i) {
        User user = userBiz.getByIdWithCache(i.getUserId());
        if (user != null) {
            i.setUserName(user.getName());
        }
    }

    public void updateMine(UserDevice entity) {
        String currentUserId = getCurrentUserId();
        long count = lambdaQuery()
                .eq(UserDevice::getDeviceId, entity.getDeviceId())
                .isNull(UserDevice::getClientType)
                .count();

        if (count > 1) {
            throw new BuzzException("重复设备编码，请联系管理员");
        }

        if (count == 0) {
            long existingClientCount = lambdaQuery()
                    .eq(UserDevice::getDeviceId, entity.getDeviceId())
                    .count();
            if (existingClientCount > 0) {
                throw new BuzzException("设备已登记为客户端设备，不能通过旧设备接口修改");
            }
            entity.setUserId(currentUserId);
            entity.setClientType(null);
            entity.setEnable(faSetting.getApp().isDeviceDefaultAllow()); // 默认不允许访问
            entity.setLastOnlineTime(new Date());
            this.save(entity);
            return;
        }

        UserDevice entityDB = lambdaQuery()
                .eq(UserDevice::getDeviceId, entity.getDeviceId())
                .isNull(UserDevice::getClientType)
                .one();
        if (!currentUserId.equals(entityDB.getUserId())) {
            throw new BuzzException("设备已登记到其他用户，不允许转移归属");
        }
        entityDB.setModel(entity.getModel());
        entityDB.setManufacturer(entity.getManufacturer());
        entityDB.setOs(entity.getOs());
        entityDB.setOsVersion(entity.getOsVersion());
        this.updateById(entityDB);
    }

    /** 登录成功后按用户、客户端类型和客户端实例 ID 维护设备登记。 */
    public void registerClientOnLogin(User user, String clientType, String clientInstanceId,
                                      String model, String manufacturer, String os, String osVersion) {
        if (user == null || StrUtil.hasBlank(user.getId(), clientType, clientInstanceId)) return;

        UserDevice entity = lambdaQuery()
                .eq(UserDevice::getUserId, user.getId())
                .eq(UserDevice::getClientType, clientType)
                .eq(UserDevice::getDeviceId, clientInstanceId)
                .one();
        Date now = new Date();
        if (entity == null) {
            entity = new UserDevice();
            entity.setUserId(user.getId());
            entity.setClientType(clientType);
            entity.setDeviceId(clientInstanceId);
            entity.setEnable(faSetting.getApp().isDeviceDefaultAllow());
        }
        applyClientMetadata(entity, model, manufacturer, os, osVersion);
        entity.setLastOnlineTime(now);

        if (entity.getId() == null) save(entity);
        else updateById(entity);
    }

    /**
     * Enroll or renew the MobileX device trust after password validation, before creating the login session.
     * Explicitly revoked devices remain blocked until a future verified recovery flow.
     */
    @Transactional(rollbackFor = Exception.class)
    public String trustClientOnPasswordLogin(User user, String clientType, String clientInstanceId,
                                             String model, String manufacturer, String os, String osVersion,
                                             String presentedTrustToken) {
        if (user == null || StrUtil.hasBlank(user.getId(), clientType, clientInstanceId)) {
            throw new BuzzException("无法识别当前设备，请更新应用后重试");
        }

        List<UserDevice> matches = lambdaQuery()
                .eq(UserDevice::getUserId, user.getId())
                .eq(UserDevice::getClientType, clientType)
                .eq(UserDevice::getDeviceId, clientInstanceId)
                .list();
        if (matches.size() > 1) {
            throw new BuzzException("设备登记异常，请联系管理员");
        }

        UserDevice device = matches.isEmpty() ? null : matches.get(0);
        if (device != null && device.getTrustRevokedAt() != null) {
            throw new BuzzException("该设备已被设置为不信任，暂不允许通过密码登录");
        }

        Date now = new Date();
        if (device == null) {
            device = new UserDevice();
            device.setUserId(user.getId());
            device.setClientType(clientType);
            device.setDeviceId(clientInstanceId);
            device.setEnable(faSetting.getApp().isDeviceDefaultAllow());
        }

        String trustToken = presentedTrustToken;
        if (!hasValidTrustToken(device, presentedTrustToken, now)) {
            trustToken = createTrustToken();
            device.setTrustTokenHash(hashTrustToken(trustToken));
            device.setTrustedAt(now);
            device.setTrustExpiresAt(null);
        }
        applyClientMetadata(device, model, manufacturer, os, osVersion);
        device.setLastOnlineTime(now);
        if (device.getId() == null) save(device);
        else updateById(device);
        return trustToken;
    }

    private boolean hasValidTrustToken(UserDevice device, String presentedTrustToken, Date now) {
        if (device == null || StrUtil.isBlank(device.getTrustTokenHash()) || StrUtil.isBlank(presentedTrustToken)) {
            return false;
        }
        Date expiresAt = device.getTrustExpiresAt();
        if (expiresAt != null && !expiresAt.after(now)) return false;
        byte[] expected = device.getTrustTokenHash().getBytes(StandardCharsets.UTF_8);
        byte[] actual = hashTrustToken(presentedTrustToken).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }

    private String createTrustToken() {
        byte[] bytes = new byte[32];
        TRUST_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashTrustToken(String trustToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(trustToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("无法生成设备信任凭据", e);
        }
    }

    public UserDevice getByDeviceId(String deviceId) {
        long count = lambdaQuery()
                .eq(UserDevice::getDeviceId, deviceId)
                .isNull(UserDevice::getClientType)
                .count();

        if (count > 1) {
            throw new BuzzException("重复设备编码，请联系管理员");
        }

        if (count == 0) {
            return null;
        }

        UserDevice entityDB = lambdaQuery()
                .eq(UserDevice::getDeviceId, deviceId)
                .isNull(UserDevice::getClientType)
                .one();
        return entityDB;
    }

    public void updateLastOnlineTime(int id) {
        lambdaUpdate()
                .set(UserDevice::getLastOnlineTime, LocalDateTime.now())
                .eq(UserDevice::getId, id)
                .update();
    }

    /** 查询当前用户的移动端登录设备，不返回客户端实例 ID 或信任凭据。 */
    public List<PortalLoginDeviceRetVo> listPortalLoginDevices(String currentDeviceId,
                                                                String model, String manufacturer,
                                                                String os, String osVersion) {
        String userId = getCurrentUserId();
        String normalizedDeviceId = normalizeDeviceHeader(currentDeviceId, 128);
        refreshCurrentDeviceMetadata(
                userId,
                normalizedDeviceId,
                normalizeDeviceHeader(model, 128),
                normalizeDeviceHeader(manufacturer, 128),
                normalizeDeviceHeader(os, 64),
                normalizeDeviceHeader(osVersion, 64)
        );
        return baseMapper.selectPortalLoginDevices(
                userId,
                TelemetryClientTypeEnum.MOBILE.getValue(),
                normalizedDeviceId
        );
    }

    private String normalizeDeviceHeader(String value, int maxLength) {
        String normalized = StrUtil.trim(value);
        return StrUtil.isBlank(normalized) || normalized.length() > maxLength ? null : normalized;
    }

    private void refreshCurrentDeviceMetadata(String userId, String deviceId,
                                              String model, String manufacturer, String os, String osVersion) {
        if (StrUtil.isBlank(deviceId)) return;
        UserDevice device = lambdaQuery()
                .eq(UserDevice::getUserId, userId)
                .eq(UserDevice::getClientType, TelemetryClientTypeEnum.MOBILE.getValue())
                .eq(UserDevice::getDeviceId, deviceId)
                .one();
        if (device != null && applyClientMetadata(device, model, manufacturer, os, osVersion)) {
            updateById(device);
        }
    }

    private boolean applyClientMetadata(UserDevice device, String model, String manufacturer,
                                        String os, String osVersion) {
        boolean changed = false;
        if (StrUtil.isNotBlank(model) && !model.equals(device.getModel())) {
            device.setModel(model);
            changed = true;
        }
        if (StrUtil.isNotBlank(manufacturer) && !manufacturer.equals(device.getManufacturer())) {
            device.setManufacturer(manufacturer);
            changed = true;
        }
        if (StrUtil.isNotBlank(os) && !os.equals(device.getOs())) {
            device.setOs(os);
            changed = true;
        }
        if (StrUtil.isNotBlank(osVersion) && !osVersion.equals(device.getOsVersion())) {
            device.setOsVersion(osVersion);
            changed = true;
        }
        return changed;
    }

}
