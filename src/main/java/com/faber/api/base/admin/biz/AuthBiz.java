package com.faber.api.base.admin.biz;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
//import com.alicp.jetcache.Cache;
//import com.alicp.jetcache.CacheManager;
//import com.alicp.jetcache.anno.CacheType;
//import com.alicp.jetcache.template.QuickConfig;
import com.faber.api.base.admin.entity.LogLogin;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.entity.UserToken;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.api.base.telemetry.service.TelemetryService;
import com.faber.config.utils.user.LoginReqVo;
import com.faber.config.auth.OnlineUserTracker;
import com.faber.core.constant.CommonConstants;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.service.LogoutService;
import com.faber.core.utils.IpUtils;
import com.faber.core.utils.RequestUtils;
import com.faber.core.vo.utils.IpAddr;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

@Service
@Slf4j
public class AuthBiz implements LogoutService {

    @Resource UserBiz userBiz;
    @Resource UserTokenBiz userTokenBiz;
    @Resource UserDeviceBiz userDeviceBiz;
    @Resource LogLoginBiz logLoginBiz;
    @Resource OnlineUserTracker onlineUserTracker;

//    @Autowired
//    private CacheManager cacheManager;
//
//    /** 记录用户登录来源 */
//    private Cache<String, String> userTokenFromCache;

    @PostConstruct
    public void init() {
//        QuickConfig qc = QuickConfig.newBuilder("oauthCache:")
////                .expire(Duration.ofSeconds(-1))
//                .cacheType(CacheType.BOTH) // two level cache
////                .localLimit(50)
//                .syncLocal(true) // invalidate local cache in all jvm process after update
//                .build();
//        userTokenFromCache = cacheManager.getOrCreateCache(qc);
    }

    /**
     * web登录，返回token
     * @param loginReqVo
     * @return
     * @throws Exception
     */
    public SaTokenInfo login(LoginReqVo loginReqVo) {
        User user = userBiz.validate(loginReqVo.getUsername(), loginReqVo.getPassword());
        requireAdminAccess(user);
        return login(user, "web");
    }

    public SaTokenInfo portalLogin(LoginReqVo loginReqVo) {
        User user = userBiz.validate(loginReqVo.getUsername(), loginReqVo.getPassword());
        return login(user, "portal");
    }

    public SaTokenInfo loginByToken(String apiToken) {
        UserToken userToken = userTokenBiz.getById(apiToken);
        if (userToken != null && userToken.getValid()) {
            String userId = userToken.getUserId();
            User user = userBiz.getById(userId);
            requireAdminAccess(user);
            return login(user, "web");
        }
        throw new BuzzException("token error");
    }

    /**
     * 登录，返回登录成功token
     * @param user 登录用户
     * @param source 登录来源：web/app
     * @return
     */
    public SaTokenInfo login(User user, String source) {
        // 将用户放入上下文
        BaseContextHandler.setUserId(user.getId());
        BaseContextHandler.setName(user.getName());
        BaseContextHandler.setUsername(user.getUsername());
        BaseContextHandler.setLogin(true);

        // 记录登录日志
        LogLogin logLogin = new LogLogin();

        logLogin.setAgent(RequestUtils.getAgent());

        // 解析agent字符串
        UserAgent ua = UserAgentUtil.parse(logLogin.getAgent());
        ClientIdentity clientIdentity = resolveClientIdentity(source);
        logLogin.setOs(ua.getOs().toString());
        logLogin.setBrowser(ua.getBrowser().toString());
        logLogin.setVersion(ua.getVersion());
        logLogin.setMobile(ua.isMobile());
        logLogin.setClientType(clientIdentity.clientType());
        logLogin.setDeviceId(clientIdentity.clientInstanceId());

        // 获取IP地址
        IpAddr ipAddr = IpUtils.getIpAddrByApi(BaseContextHandler.getIp());
        if (ipAddr != null) {
            logLogin.setPro(ipAddr.getPro());
            logLogin.setCity(ipAddr.getCity());
            logLogin.setAddr(ipAddr.getAddr());
        }

        logLoginBiz.save(logLogin);

        if (clientIdentity.clientType() != null && clientIdentity.clientInstanceId() != null) {
            try {
                userDeviceBiz.registerClientOnLogin(user, clientIdentity.clientType(),
                        clientIdentity.clientInstanceId(), ua.getOs().toString());
            } catch (Exception e) {
                // 设备登记仅用于在线设备展示，不影响已通过认证的登录。
                log.warn("登录设备登记失败 userId={} clientType={}",
                        user.getId(), clientIdentity.clientType(), e);
            }
        }

        // 使用sa-token登录框架
        StpUtil.login(user.getId(), source);
        SaTokenInfo tokenInfo = StpUtil.getTokenInfo();
        if ("web".equals(source)) onlineUserTracker.touch(tokenInfo.getTokenValue(), user, true);
        return tokenInfo;
    }

    private ClientIdentity resolveClientIdentity(String source) {
        HttpServletRequest request = currentRequest();
        String clientInstanceId = readHeader(request, "FaClientInstanceId", 128);
        String clientType = readHeader(request, TelemetryService.HEADER_CLIENT_TYPE, 16);
        if (clientType != null) {
            try {
                clientType = TelemetryClientTypeEnum.valueOf(clientType.toUpperCase(Locale.ROOT)).getValue();
            } catch (IllegalArgumentException e) {
                clientType = null;
            }
        }

        if (clientType == null && request != null) {
            String from = readHeader(request, CommonConstants.FA_FROM, 32);
            if (CommonConstants.FaFrom.FaApp.equalsIgnoreCase(from)) {
                clientType = TelemetryClientTypeEnum.MOBILE.getValue();
            } else if (CommonConstants.FaFrom.FaWeb.equalsIgnoreCase(from)) {
                clientType = TelemetryClientTypeEnum.WEB.getValue();
            }
        }
        if (clientType == null) {
            if ("portal".equalsIgnoreCase(source)) clientType = TelemetryClientTypeEnum.MOBILE.getValue();
            else if ("web".equalsIgnoreCase(source)) clientType = TelemetryClientTypeEnum.WEB.getValue();
        }
        return new ClientIdentity(clientType, clientInstanceId);
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String readHeader(HttpServletRequest request, String name, int maxLength) {
        if (request == null) return null;
        String value = StrUtil.trim(request.getHeader(name));
        if (StrUtil.isBlank(value) || value.length() > maxLength) return null;
        return value;
    }

    private record ClientIdentity(String clientType, String clientInstanceId) {}

    private void requireAdminAccess(User user) {
        if (!Boolean.TRUE.equals(user.getAdminEnabled())) {
            throw new BuzzException("当前账号未开通后台访问权限");
        }
    }

    @Override
    public String logout() {
        String token = StpUtil.getTokenValue();
        StpUtil.logout();
        onlineUserTracker.remove(token);
        return LogoutService.super.logout();
    }

}
