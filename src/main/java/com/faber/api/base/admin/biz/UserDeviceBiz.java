package com.faber.api.base.admin.biz;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.entity.UserDevice;
import com.faber.api.base.admin.mapper.UserDeviceMapper;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.api.portal.auth.vo.PortalLoginDeviceRetVo;
import com.faber.core.constant.FaSetting;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.auth.UserTokenException;
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
    private static final String TOKEN_SESSION_CLIENT_TYPE = "fa.device-trust.client-type";
    private static final String TOKEN_SESSION_CLIENT_INSTANCE_ID = "fa.device-trust.client-instance-id";
    private static final String TOKEN_SESSION_BINDING_VERSION = "fa.device-trust.binding-version";
    private static final String DEVICE_BINDING_VERSION = "2";

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

    /** Bind a newly created MobileX login session to its app installation. */
    public void bindCurrentMobileDeviceSession(String clientType, String clientInstanceId) {
        if (!TelemetryClientTypeEnum.MOBILE.getValue().equals(clientType)
                || StrUtil.isBlank(clientInstanceId)) return;
        String token = StpUtil.getTokenValue();
        if (StrUtil.isBlank(token)) return;
        SaSession tokenSession = StpUtil.getTokenSessionByToken(token);
        if (tokenSession == null) return;
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) rejectMobileSession(token, "登录会话已失效，请重新登录");
        UserDevice device = findMobileClientDevice(String.valueOf(loginId), clientInstanceId);
        if (device == null || device.getTrustRevokedAt() != null) {
            rejectMobileSession(token, "当前设备已被设置为不信任，请重新登录");
        }
        tokenSession.set(TOKEN_SESSION_CLIENT_TYPE, clientType);
        tokenSession.set(TOKEN_SESSION_CLIENT_INSTANCE_ID, clientInstanceId);
        tokenSession.set(TOKEN_SESSION_BINDING_VERSION, DEVICE_BINDING_VERSION);
    }

    /** Reject legacy shared App tokens and validate the device bound at login. */
    public void validateMobileDeviceSession(String requestClientType, String requestClientInstanceId) {
        String token = StpUtil.getTokenValue();
        if (StrUtil.isBlank(token) || !"portal".equals(StpUtil.getLoginDeviceByToken(token))) return;
        Object loginId = StpUtil.getLoginIdByToken(token);
        if (loginId == null) return;

        SaSession tokenSession = StpUtil.getTokenSessionByToken(token);
        if (tokenSession == null) return;
        String boundClientType = sessionString(tokenSession.get(TOKEN_SESSION_CLIENT_TYPE));
        String boundClientInstanceId = sessionString(tokenSession.get(TOKEN_SESSION_CLIENT_INSTANCE_ID));
        String requestType = StrUtil.trimToNull(requestClientType);
        String requestDeviceId = normalizeDeviceHeader(requestClientInstanceId, 128);

        if (!TelemetryClientTypeEnum.MOBILE.getValue().equalsIgnoreCase(boundClientType)
                && !TelemetryClientTypeEnum.MOBILE.getValue().equalsIgnoreCase(requestType)) return;
        if (!DEVICE_BINDING_VERSION.equals(sessionString(tokenSession.get(TOKEN_SESSION_BINDING_VERSION)))) {
            rejectMobileSession(token, "登录设备管理已更新，请重新登录");
        }

        if (TelemetryClientTypeEnum.MOBILE.getValue().equalsIgnoreCase(boundClientType)) {
            if (StrUtil.isBlank(boundClientInstanceId)) {
                rejectMobileSession(token, "登录会话设备信息不完整，请重新登录");
            }
            if ((StrUtil.isNotBlank(requestType)
                    && !TelemetryClientTypeEnum.MOBILE.getValue().equalsIgnoreCase(requestType))
                    || (StrUtil.isNotBlank(requestDeviceId) && !boundClientInstanceId.equals(requestDeviceId))) {
                // An inconsistent request must not invalidate another device's legitimate token.
                throw new UserTokenException("登录会话与当前设备不匹配，请重新登录");
            }
            UserDevice boundDevice = findMobileClientDevice(String.valueOf(loginId), boundClientInstanceId);
            if (boundDevice == null) rejectMobileSession(token, "当前设备登记已失效，请重新登录");
            if (boundDevice.getTrustRevokedAt() != null) {
                rejectMobileSession(token, "当前设备已被设置为不信任，请重新登录");
            }
            return;
        } else {
            rejectMobileSession(token, "登录会话设备信息不完整，请重新登录");
        }
    }

    /** Revoke one of the current user's trusted MobileX devices and its known sessions. */
    public void revokePortalDeviceTrust(Integer deviceRecordId) {
        if (deviceRecordId == null || deviceRecordId <= 0) throw new BuzzException("设备参数无效");
        String userId = getCurrentUserId();
        UserDevice device = lambdaQuery()
                .eq(UserDevice::getId, deviceRecordId)
                .eq(UserDevice::getUserId, userId)
                .eq(UserDevice::getClientType, TelemetryClientTypeEnum.MOBILE.getValue())
                .one();
        if (device == null) throw new BuzzException("设备不存在或无权操作");
        String currentDeviceId = getCurrentPortalMobileDeviceId();
        if (StrUtil.isNotBlank(currentDeviceId) && currentDeviceId.equals(device.getDeviceId())) {
            throw new BuzzException("当前登录设备不能设为不信任，请使用其他设备操作");
        }

        if (device.getTrustRevokedAt() == null) {
            Date now = new Date();
            if (StrUtil.isBlank(device.getTrustTokenHash())
                    || (device.getTrustExpiresAt() != null && !device.getTrustExpiresAt().after(now))) {
                throw new BuzzException("设备当前不是可信状态，请刷新设备列表");
            }
            boolean updated = lambdaUpdate()
                    .set(UserDevice::getTrustTokenHash, (String) null)
                    .set(UserDevice::getTrustRevokedAt, now)
                    .eq(UserDevice::getId, deviceRecordId)
                    .eq(UserDevice::getUserId, userId)
                    .eq(UserDevice::getClientType, TelemetryClientTypeEnum.MOBILE.getValue())
                    .isNull(UserDevice::getTrustRevokedAt)
                    .update();
            if (!updated) {
                UserDevice latest = lambdaQuery()
                        .eq(UserDevice::getId, deviceRecordId)
                        .eq(UserDevice::getUserId, userId)
                        .eq(UserDevice::getClientType, TelemetryClientTypeEnum.MOBILE.getValue())
                        .one();
                if (latest == null || latest.getTrustRevokedAt() == null) {
                    throw new BuzzException("设备状态已变化，请刷新设备列表");
                }
            }
        }

        kickoutPortalDeviceSessions(userId, device.getDeviceId());
    }

    private void kickoutPortalDeviceSessions(String userId, String deviceId) {
        for (String token : StpUtil.getTokenValueListByLoginId(userId, "portal")) {
            SaSession tokenSession = StpUtil.getTokenSessionByToken(token);
            if (tokenSession == null) continue;
            String clientType = sessionString(tokenSession.get(TOKEN_SESSION_CLIENT_TYPE));
            String clientInstanceId = sessionString(tokenSession.get(TOKEN_SESSION_CLIENT_INSTANCE_ID));
            if (TelemetryClientTypeEnum.MOBILE.getValue().equals(clientType)
                    && deviceId.equals(clientInstanceId)) {
                StpUtil.kickoutByTokenValue(token);
            }
        }
    }

    private UserDevice findMobileClientDevice(String userId, String clientInstanceId) {
        List<UserDevice> matches = lambdaQuery()
                .eq(UserDevice::getUserId, userId)
                .eq(UserDevice::getClientType, TelemetryClientTypeEnum.MOBILE.getValue())
                .eq(UserDevice::getDeviceId, clientInstanceId)
                .list();
        if (matches.size() > 1) throw new UserTokenException("设备登记异常，请重新登录");
        return matches.isEmpty() ? null : matches.get(0);
    }

    private String sessionString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    private String getCurrentPortalMobileDeviceId() {
        String token = StpUtil.getTokenValue();
        if (StrUtil.isBlank(token) || !"portal".equals(StpUtil.getLoginDeviceByToken(token))) return null;
        SaSession tokenSession = StpUtil.getTokenSessionByToken(token);
        if (tokenSession == null
                || !TelemetryClientTypeEnum.MOBILE.getValue()
                    .equals(sessionString(tokenSession.get(TOKEN_SESSION_CLIENT_TYPE)))) return null;
        return sessionString(tokenSession.get(TOKEN_SESSION_CLIENT_INSTANCE_ID));
    }

    private void rejectMobileSession(String token, String message) {
        StpUtil.kickoutByTokenValue(token);
        throw new UserTokenException(message);
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
    public List<PortalLoginDeviceRetVo> listPortalLoginDevices(String model, String manufacturer,
                                                                String os, String osVersion) {
        String userId = getCurrentUserId();
        String normalizedDeviceId = normalizeDeviceHeader(getCurrentPortalMobileDeviceId(), 128);
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
