package com.faber.api.base.security.biz;

import com.faber.api.base.security.config.RequestCryptoProperties;
import com.faber.core.crypto.CryptoEnvelope;
import com.faber.core.crypto.RequestCrypto;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaRedisUtils;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.type.LogicalType;
import jakarta.validation.Validator;
import org.redisson.api.RBucket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
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
    private static final int CHALLENGE_SECONDS = 120;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RequestCryptoProperties properties;
    private final FaRedisUtils redis;
    private final Validator validator;
    private final ObjectMapper json;
    private final RSAPrivateCrtKey privateKey;
    private final String publicKey;

    public RequestCryptoBiz(RequestCryptoProperties properties, FaRedisUtils redis, Validator validator,
                            ObjectMapper objectMapper, Environment environment) {
        this.properties = properties;
        this.redis = redis;
        this.validator = validator;
        this.json = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION)
                .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS);
        this.json.coercionConfigFor(LogicalType.Textual)
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);
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
        return new PublicKeyInfo(1, properties.getKid(), publicKey);
    }

    public Preparation prepare(String purpose, String userId) {
        requireDemo(purpose, userId);
        if (!properties.getReplay().isEnabled()) {
            return new Preparation(1, properties.getKid(), false, null, null);
        }
        byte[] random = new byte[32];
        RANDOM.nextBytes(random);
        String id = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        try {
            bucket(id).set(binding(userId), CHALLENGE_SECONDS, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            throw new BuzzException("防重放服务暂不可用");
        }
        return new Preparation(1, properties.getKid(), true, id,
                Long.toString(System.currentTimeMillis() + CHALLENGE_SECONDS * 1000L));
    }

    public <T> T decryptDemo(CryptoEnvelope envelope, String userId, Class<T> type) {
        requireDemo(envelope == null ? null : envelope.purpose(), userId);
        if (!properties.getKid().equals(envelope.kid())) throw new BuzzException("加密密钥不支持，请重新准备请求");
        boolean replay = properties.getReplay().isEnabled();
        if (replay) {
            if (envelope.challengeId() == null || !envelope.challengeId().matches("[A-Za-z0-9_-]{43}")) {
                throw new BuzzException("缺少有效挑战，请重新准备请求");
            }
            if (!binding(userId).equals(readChallenge(envelope.challengeId(), false))) {
                throw new BuzzException("挑战已过期、已使用或不匹配");
            }
        } else if (envelope.challengeId() != null) {
            throw new BuzzException("防重放配置已变化，请重新准备请求");
        }
        T body;
        try {
            byte[] plaintext = RequestCrypto.decrypt(envelope, privateKey, "POST", DEMO_PATH);
            body = json.readValue(plaintext, type);
            if (body == null || !validator.validate(body).isEmpty()) throw new IllegalArgumentException();
        } catch (Exception e) {
            throw new BuzzException("加密信封或业务数据无效");
        }
        if (replay && !binding(userId).equals(readChallenge(envelope.challengeId(), true))) {
            throw new BuzzException("挑战已过期、已使用或不匹配");
        }
        return body;
    }

    private void requireDemo(String purpose, String userId) {
        if (!DEMO_PURPOSE.equals(purpose)) throw new BuzzException("不支持的加密用途");
        if (userId == null || userId.isBlank()) throw new BuzzException("请先登录");
    }

    private String binding(String userId) {
        return properties.getKid() + ":" + DEMO_PURPOSE + ":POST:" + DEMO_PATH + ":" + userId;
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

    public record PublicKeyInfo(int v, String kid, String publicKey) {}
    public record Preparation(int v, String kid, boolean replayEnabled, String challengeId, String expiresAt) {}
}
