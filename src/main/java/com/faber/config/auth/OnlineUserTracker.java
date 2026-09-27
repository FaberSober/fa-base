package com.faber.config.auth;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.api.base.admin.entity.User;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.utils.RequestUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Slf4j
@Component
public class OnlineUserTracker {
    @Resource
    private OnlineUserStore store;
    @Value("${fa.online-user.touch-interval-seconds:30}")
    private long touchIntervalSeconds;

    public static String sessionId(String token) {
        return DigestUtil.sha256Hex(token);
    }

    /** 观测失败不能使一次本已成功的登录或业务请求失败。 */
    public void touch(String token, User user, boolean login) {
        touch(token, user, login, null, null);
    }

    /** 设备标识仅用于展示，不参与认证或会话撤销。 */
    public void touch(String token, User user, boolean login, String clientType, String clientInstanceId) {
        try {
            if (StrUtil.isBlank(token) || user == null) return;
            String id = sessionId(token);
            OnlineUserSession previous = store.get(id);
            long now = System.currentTimeMillis();
            if (!login && previous != null
                    && now - previous.getLastAccessTime() < Math.max(1, touchIntervalSeconds) * 1000) return;
            if (!user.getId().equals(StpUtil.getLoginIdByToken(token))
                    || !"web".equals(StpUtil.getLoginDeviceByToken(token))) return;
            long timeout = StpUtil.getTokenTimeout(token);
            if (timeout != -1 && timeout <= 0) return;

            OnlineUserSession session = new OnlineUserSession();
            session.setId(id);
            session.setToken(token);
            session.setUserId(user.getId());
            session.setUsername(user.getUsername());
            session.setName(user.getName());
            // 共享 Token 保留原登录时间；部署前会话首次观测时不能伪造登录时间。
            session.setLoginTime(previous != null ? previous.getLoginTime() : login ? now : null);
            session.setLastAccessTime(now);
            session.setIp(BaseContextHandler.getIp());
            UserAgent ua = UserAgentUtil.parse(StrUtil.blankToDefault(RequestUtils.getAgent(), ""));
            session.setBrowser(ua.getBrowser().toString());
            session.setOs(ua.getOs().toString());
            session.setExpiresAt(timeout == -1 ? null : now + timeout * 1000);
            store.put(session, timeout);
            store.putClientInfo(id, normalizeClientType(clientType), normalizeClientInstanceId(clientInstanceId), timeout);
            if (previous == null) store.invalidate();
        } catch (Exception e) {
            // 不输出异常内容，避免第三方异常携带 Token。
            log.warn("在线会话采集失败，异常类型：{}", e.getClass().getSimpleName());
        }
    }

    private String normalizeClientType(String clientType) {
        if (StrUtil.isBlank(clientType)) return TelemetryClientTypeEnum.WEB.getValue();
        try {
            return TelemetryClientTypeEnum.valueOf(clientType.trim().toUpperCase(Locale.ROOT)).getValue();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String normalizeClientInstanceId(String clientInstanceId) {
        if (StrUtil.isBlank(clientInstanceId)) return null;
        String value = clientInstanceId.trim();
        return value.length() <= 128 ? value : null;
    }

    public void remove(String token) {
        if (StrUtil.isBlank(token)) return;
        try {
            store.remove(sessionId(token));
            store.invalidate();
        } catch (Exception e) {
            log.warn("在线会话清理失败，异常类型：{}", e.getClass().getSimpleName());
        }
    }
}
