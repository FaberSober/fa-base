package com.faber.api.base.security.biz;

import com.faber.api.base.security.config.RequestCryptoProperties;
import com.faber.core.crypto.RequestCrypto;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaRedisUtils;
import org.redisson.api.RBucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.util.Collections;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import com.faber.core.context.BaseContextHandler;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@ConditionalOnProperty(prefix = "fa.security.crypto", name = "enabled", havingValue = "true")
public class RequestCryptoBiz {

    public static final String DEMO_PURPOSE = "CRYPTO_DEMO";
    public static final String DEMO_PATH = "/api/base/demo/crypto/roundtrip";
    public static final String DEMO_PERMISSION = "/admin/demo/crypto";
    public static final String ADMIN_LOGIN = "ADMIN_LOGIN";
    public static final String PORTAL_LOGIN = "PORTAL_LOGIN";
    public static final String ADMIN_LOGIN_PATH = "/api/base/admin/auth/loginV2";
    public static final String PORTAL_LOGIN_PATH = "/api/portal/auth/loginV2";
    private static final int CHALLENGE_SECONDS = 120;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RequestCryptoProperties properties;
    private final FaRedisUtils redis;
    private final RSAPrivateCrtKey privateKey;
    private final String publicKey;

    public RequestCryptoBiz(RequestCryptoProperties properties, FaRedisUtils redis, Environment environment) {
        this.properties = properties;
        this.redis = redis;
        try {
            if (properties.getKid() == null || !properties.getKid().matches("[A-Za-z0-9._-]{1,64}")) {
                throw new IllegalArgumentException();
            }
            String location = properties.getPrivateKeyFile();
            Resource resource;
            if (location.startsWith("classpath:")) {
                if (!environment.acceptsProfiles(Profiles.of("dev & !prod"))) throw new IllegalArgumentException();
                resource = new ClassPathResource(location.substring("classpath:".length()));
            } else {
                resource = new FileSystemResource(location);
            }
            byte[] encoded;
            try (var input = resource.getInputStream()) {
                encoded = input.readNBytes(16385);
            }
            if (encoded.length > 16384) throw new IllegalArgumentException();
            String pem = new String(encoded, StandardCharsets.US_ASCII).replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
            KeyFactory factory = KeyFactory.getInstance("RSA");
            this.privateKey = (RSAPrivateCrtKey) factory.generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pem)));
            if (privateKey.getModulus().bitLength() != 2048) throw new IllegalArgumentException();
            this.publicKey = Base64.getEncoder().encodeToString(factory.generatePublic(
                    new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent())).getEncoded());
        } catch (Exception e) {
            // 不附带原异常，避免配置绑定错误或文件内容进入启动日志。
            throw new IllegalStateException("请求加密配置无效，请检查 kid 和 RSA-2048 PKCS#8 私钥文件；classpath 测试密钥仅允许 dev 环境");
        }
    }

    public PublicKeyInfo publicKey() {
        return new PublicKeyInfo(2, properties.getKid(), publicKey);
    }

    private static final List<Target> TARGETS = List.of(
            new Target(ADMIN_LOGIN, ADMIN_LOGIN_PATH, "POST", "password", null),
            new Target(PORTAL_LOGIN, PORTAL_LOGIN_PATH, "POST", "password", null),
            new Target(DEMO_PURPOSE, DEMO_PATH, "POST", "text", DEMO_PERMISSION));

    public Target target(String path, String method) {
        return TARGETS.stream().filter(target -> target.path().equals(path) && target.method().equals(method))
                .findFirst().orElseThrow(() -> new BuzzException("不支持的加密目标"));
    }

    public Preparation prepare(String path, String method, String userId, boolean login) {
        Target target = target(path, method);
        if (login != target.login()) throw new BuzzException("不支持的加密目标");
        if (!login) requireUser(userId);
        String challenge = null;
        String expires = null;
        if (properties.getReplay().isEnabled()) {
            byte[] random = new byte[32];
            RANDOM.nextBytes(random);
            challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
            try {
                bucket(challenge).set(binding(target, login ? null : userId), CHALLENGE_SECONDS, TimeUnit.SECONDS);
            } catch (RuntimeException e) {
                throw new BuzzException("防重放服务暂不可用");
            }
            expires = Long.toString(System.currentTimeMillis() + CHALLENGE_SECONDS * 1000L);
        }
        return new Preparation(2, properties.getKid(), target.purpose(), List.of(target.field()),
                properties.getReplay().isEnabled(), challenge, expires);
    }

    public String decryptField(String value, HttpServletRequest request, String field) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        Target target = target(path, request.getMethod());
        if (!target.field().equals(field)) throw new BuzzException("不支持的加密字段");
        String userId = target.login() ? null : BaseContextHandler.getUserId();
        if (!target.login()) requireUser(userId);
        String version = header(request, "FaCryptoVersion");
        String kid = header(request, "FaCryptoKid");
        String challenge = header(request, "FaCryptoChallengeId");
        if (!"2".equals(version)) throw new BuzzException("字段加密协议不支持");
        if (!properties.getKid().equals(kid)) throw new BuzzException("加密密钥不支持，请重新准备请求");
        boolean replay = properties.getReplay().isEnabled();
        if (replay) {
            if (challenge == null || !challenge.matches("[A-Za-z0-9_-]{43}")) throw new BuzzException("缺少有效挑战，请重新准备请求");
            if (!binding(target, userId).equals(readChallenge(challenge, false))) throw new BuzzException("挑战已过期、已使用或不匹配");
        } else if (challenge != null) {
            throw new BuzzException("防重放配置已变化，请重新准备请求");
        }
        String plaintext;
        try {
            byte[] bytes = RequestCrypto.decryptField(value, privateKey, kid, challenge, target.purpose(), target.method(), path, field);
            plaintext = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (Exception e) {
            throw new BuzzException("字段密文无效");
        }
        if (replay && !binding(target, userId).equals(readChallenge(challenge, true))) throw new BuzzException("挑战已过期、已使用或不匹配");
        return plaintext;
    }

    private String header(HttpServletRequest request, String name) {
        var values = Collections.list(request.getHeaders(name));
        if (values.size() > 1) throw new BuzzException("加密请求头重复");
        return values.isEmpty() ? null : values.get(0);
    }

    private void requireUser(String userId) {
        if (userId == null || userId.isBlank()) throw new BuzzException("请先登录");
    }

    private String binding(Target target, String userId) {
        return "2:" + properties.getKid() + ":" + target.purpose() + ":" + target.method() + ":" + target.path() + ":" + target.field() + ":" + (userId == null ? "" : userId);
    }

    private RBucket<String> bucket(String id) {
        return redis.getRedisson().getBucket(redis.buildKey("crypto:challenge:" + id));
    }

    private String readChallenge(String id, boolean consume) {
        try {
            RBucket<String> bucket = bucket(id);
            return consume ? bucket.getAndDelete() : bucket.get();
        } catch (RuntimeException e) {
            throw new BuzzException("防重放服务暂不可用");
        }
    }

    // shortcut: 每个目标当前只登记一个加密字段；同请求多字段需统一解密后消费挑战。
    public record Target(String purpose, String path, String method, String field, String permission) {
        public boolean login() { return ADMIN_LOGIN.equals(purpose) || PORTAL_LOGIN.equals(purpose); }
    }
    public record PublicKeyInfo(int v, String kid, String publicKey) {}
    public record Preparation(int v, String kid, String purpose, List<String> fields, boolean replayEnabled, String challengeId, String expiresAt) {}
}
