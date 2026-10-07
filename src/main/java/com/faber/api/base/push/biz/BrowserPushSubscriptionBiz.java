package com.faber.api.base.push.biz;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.faber.api.base.push.entity.BrowserPushSubscription;
import com.faber.api.base.push.mapper.BrowserPushSubscriptionMapper;
import com.faber.api.base.push.vo.req.BrowserPushSubscriptionReqVo;
import com.faber.api.base.push.vo.req.BrowserPushTestSendReqVo;
import com.faber.api.base.push.vo.ret.BrowserPushSubscriptionStatusRetVo;
import com.faber.api.base.push.vo.ret.BrowserPushTestSendRetVo;
import com.faber.api.base.push.webpush.BrowserPushProperties;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import jakarta.annotation.Resource;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

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

    public BrowserPushTestSendRetVo sendTestToCurrentUser(BrowserPushTestSendReqVo reqVo) {
        List<BrowserPushSubscription> subscriptions = lambdaQuery()
                .eq(BrowserPushSubscription::getUserId, getCurrentUserId())
                .list();
        if (subscriptions.isEmpty()) {
            throw new BuzzException("当前登录用户没有已同步的浏览器推送订阅");
        }

        PushService pushService = createPushService();
        String payload = JSON.toJSONString(Map.of(
                "title", reqVo.getTitle().trim(),
                "body", reqVo.getBody().trim()
        ));
        int acceptedCount = 0;
        int failedCount = 0;

        for (int index = 0; index < subscriptions.size(); index++) {
            BrowserPushSubscription subscription = subscriptions.get(index);
            try {
                Notification notification = new Notification(
                        normalizeEndpoint(subscription.getEndpoint()),
                        subscription.getP256dh(),
                        subscription.getAuth(),
                        payload.getBytes(StandardCharsets.UTF_8),
                        60
                );
                HttpResponse response = pushService.send(notification);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode >= 200 && statusCode < 300) {
                    acceptedCount++;
                } else {
                    failedCount++;
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                failedCount += subscriptions.size() - index;
                break;
            } catch (Exception exception) {
                failedCount++;
            }
        }

        return new BrowserPushTestSendRetVo(subscriptions.size(), acceptedCount, failedCount);
    }

    private PushService createPushService() {
        String publicKey = getVapidPublicKey();
        String privateKey = browserPushProperties.getVapidPrivateKey();
        if (privateKey == null || privateKey.isBlank()) {
            throw new BuzzException("未配置 Web Push VAPID 私钥，请设置 FA_PUSH_WEBPUSH_VAPID_PRIVATE_KEY");
        }
        String subject = browserPushProperties.getVapidSubject();
        if (subject == null || subject.isBlank()) {
            throw new BuzzException("未配置 Web Push VAPID 联系地址，请设置 FA_PUSH_WEBPUSH_VAPID_SUBJECT");
        }
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        try {
            return new PushService(publicKey, privateKey.trim(), subject.trim());
        } catch (GeneralSecurityException exception) {
            throw new BuzzException("Web Push VAPID 密钥格式无效，请检查公钥和私钥配置");
        }
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
        String path = uri.getPath();
        if (!"fcm.googleapis.com".equalsIgnoreCase(uri.getHost())
                || (uri.getPort() != -1 && uri.getPort() != 443)
                || path == null
                || !(path.startsWith("/fcm/send/") || path.startsWith("/wp/"))) {
            throw new BuzzException("当前仅支持 Chrome FCM Web Push endpoint");
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
