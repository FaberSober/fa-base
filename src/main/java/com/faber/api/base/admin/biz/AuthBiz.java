package com.faber.api.base.admin.biz;

import cn.hutool.core.lang.UUID;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.alicp.jetcache.Cache;
import com.alicp.jetcache.CacheManager;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.template.QuickConfig;
import com.faber.api.base.admin.entity.LogLogin;
import com.faber.api.base.admin.entity.User;
import com.faber.config.utils.user.AuthRequest;
import com.faber.config.utils.user.JwtTokenUtil;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.service.LogoutService;
import com.faber.core.utils.FaKeyUtils;
import com.faber.core.utils.FaRedisUtils;
import com.faber.core.utils.IpUtils;
import com.faber.core.utils.RequestUtils;
import com.faber.core.vo.utils.IpAddr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class AuthBiz implements LogoutService {

    @Resource
    private JwtTokenUtil jwtTokenUtil;

    @Resource
    private UserBiz userBiz;

    @Resource
    private LogLoginBiz logLoginBiz;

    @Resource
    FaRedisUtils faRedisUtils;

    @Resource
    ConfigSysBiz configSysBiz;

    @Autowired
    private CacheManager cacheManager;
    /** 记录用户登录来源 */
    private Cache<String, String> userTokenFromCache;

    @PostConstruct
    public void init() {
        QuickConfig qc = QuickConfig.newBuilder("oauthCache:")
//                .expire(Duration.ofSeconds(-1))
                .cacheType(CacheType.BOTH) // two level cache
//                .localLimit(50)
                .syncLocal(true) // invalidate local cache in all jvm process after update
                .build();
        userTokenFromCache = cacheManager.getOrCreateCache(qc);
    }

    /**
     * web登录，返回token
     * @param authRequest
     * @return
     * @throws Exception
     */
    public String login(AuthRequest authRequest) {
        User user = userBiz.validate(authRequest.getUsername(), authRequest.getPassword());
        return login(user, "web");
    }

    public String login(User user, String source) {
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
        logLogin.setOs(ua.getOs().toString());
        logLogin.setBrowser(ua.getBrowser().toString());
        logLogin.setVersion(ua.getVersion());
        logLogin.setMobile(ua.isMobile());

        // 获取IP地址
        IpAddr ipAddr = IpUtils.getIpAddrByApi(BaseContextHandler.getIp());
        if (ipAddr != null) {
            logLogin.setPro(ipAddr.getPro());
            logLogin.setCity(ipAddr.getCity());
            logLogin.setAddr(ipAddr.getAddr());
        }

        logLoginBiz.save(logLogin);

        // 登录模式
        userTokenFromCache.put(user.getId() + ":from", source);

        // 返回token：token暂时使用uuid，存放到redis缓存中
        String token = UUID.fastUUID().toString(true);
        faRedisUtils.set(FaKeyUtils.getTokenKey(token), user.getId() + "", configSysBiz.getConfig().getSafeTokenExpireHour(), TimeUnit.HOURS);

        return token;
    }


}
