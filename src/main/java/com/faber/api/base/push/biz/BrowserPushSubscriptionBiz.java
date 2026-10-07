package com.faber.api.base.push.biz;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.faber.api.base.push.entity.BrowserPushSubscription;
import com.faber.api.base.push.mapper.BrowserPushSubscriptionMapper;
import com.faber.api.base.push.vo.req.BrowserPushSubscriptionReqVo;
import com.faber.api.base.push.vo.ret.BrowserPushSubscriptionStatusRetVo;
import com.faber.api.base.push.webpush.BrowserPushProperties;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class BrowserPushSubscriptionBiz extends BaseBiz<BrowserPushSubscriptionMapper, BrowserPushSubscription> {

    @Resource
    private BrowserPushProperties browserPushProperties;

    public BrowserPushSubscriptionStatusRetVo status(String endpoint) {
        String endpointHash = hashEndpoint(normalizeEndpoint(endpoint));
        boolean subscribed = lambdaQuery()
                .eq(BrowserPushSubscription::getUserId, getCurrentUserId())
                .eq(BrowserPushSubscription::getEndpointHash, endpointHash)
                .count() > 0;
        return new BrowserPushSubscriptionStatusRetVo(subscribed);
    }

    public String getVapidPublicKey() {
        String publicKey = browserPushProperties.getVapidPublicKey();
        if (publicKey == null || publicKey.isBlank()) {
            throw new BuzzException("未配置 Web Push VAPID 公钥，请设置 fa.push.webpush.vapid-public-key");
        }
        return publicKey.trim();
    }

    @Transactional(rollbackFor = Exception.class)
    public void registerCurrentUser(BrowserPushSubscriptionReqVo reqVo) {
        String userId = getCurrentUserId();
        String endpoint = normalizeEndpoint(reqVo.getEndpoint());
        String endpointHash = hashEndpoint(endpoint);
        BrowserPushSubscription subscription = lambdaQuery()
                .eq(BrowserPushSubscription::getEndpointHash, endpointHash)
                .one();

        if (subscription == null) {
            subscription = new BrowserPushSubscription();
            subscription.setEndpointHash(endpointHash);
        }
        subscription.setUserId(userId);
        subscription.setEndpoint(endpoint);
        subscription.setP256dh(reqVo.getKeys().getP256dh().trim());
        subscription.setAuth(reqVo.getKeys().getAuth().trim());

        if (subscription.getId() == null) {
            save(subscription);
        } else {
            updateById(subscription);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unregisterCurrentUser(String endpoint) {
        String userId = getCurrentUserId();
        String endpointHash = hashEndpoint(normalizeEndpoint(endpoint));
        remove(Wrappers.<BrowserPushSubscription>lambdaQuery()
                .eq(BrowserPushSubscription::getUserId, userId)
                .eq(BrowserPushSubscription::getEndpointHash, endpointHash));
    }

    private String normalizeEndpoint(String endpoint) {
        String value = endpoint == null ? "" : endpoint.trim();
        URI uri;
        try {
            uri = URI.create(value);
        } catch (IllegalArgumentException exception) {
            throw new BuzzException("浏览器推送 endpoint 格式无效");
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new BuzzException("浏览器推送 endpoint 必须是有效的 HTTPS 地址");
        }
        return value;
    }

    private String hashEndpoint(String endpoint) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(endpoint.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }
}
