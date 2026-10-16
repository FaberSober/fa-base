package com.faber.api.base.security.biz;

import com.faber.api.base.security.config.RequestCryptoProperties;
import com.faber.api.base.admin.biz.AuthBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.security.rest.RequestCryptoLoginController;
import com.faber.core.crypto.CryptoEnvelope;
import com.faber.core.crypto.RequestCrypto;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaRedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.mock.env.MockEnvironment;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestCryptoBizTest {

    @TempDir Path directory;
    private static KeyPair pair;
    private static ValidatorFactory validators;
    private final RequestCryptoProperties properties = new RequestCryptoProperties();
    private final FaRedisUtils redis = mock(FaRedisUtils.class);
    private final RedissonClient redisson = mock(RedissonClient.class);
    @SuppressWarnings("unchecked")
    private final RBucket<String> bucket = mock(RBucket.class);
    private final AtomicReference<String> stored = new AtomicReference<>();
    private RequestCryptoBiz biz;

    @BeforeAll
    static void keys() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        pair = generator.generateKeyPair();
        validators = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidators() {
        validators.close();
    }

    @BeforeEach
    void setup() throws Exception {
        Path key = directory.resolve("test-only.pem");
        Files.writeString(key, "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(pair.getPrivate().getEncoded()) + "\n-----END PRIVATE KEY-----");
        properties.setPrivateKeyFile(key.toString());
        when(redis.getRedisson()).thenReturn(redisson);
        when(redis.buildKey(anyString())).thenAnswer(call -> "test:" + call.getArgument(0));
        when(redisson.<String>getBucket(anyString())).thenReturn(bucket);
        doAnswer(call -> { stored.set(call.getArgument(0)); return null; })
                .when(bucket).set(anyString(), eq(120L), eq(TimeUnit.SECONDS));
        when(bucket.get()).thenAnswer(call -> stored.get());
        when(bucket.getAndDelete()).thenAnswer(call -> stored.getAndSet(null));
        biz = new RequestCryptoBiz(properties, redis, validators.getValidator(), new ObjectMapper(), new MockEnvironment());
    }

    @Test
    void disabledReplayNeedsNoRedisAndAllowsResubmission() throws Exception {
        var ready = biz.prepare("CRYPTO_DEMO", "user1");
        assertFalse(ready.replayEnabled());
        assertNull(ready.challengeId());
        assertNull(ready.expiresAt());
        assertEquals(Base64.getEncoder().encodeToString(pair.getPublic().getEncoded()), biz.publicKey().publicKey());
        var envelope = encrypt(ready, "{\"text\":\"中文 🔐\\n\"}");
        assertEquals("中文 🔐\n", biz.decryptDemo(envelope, "user1", Text.class).text());
        assertEquals("中文 🔐\n", biz.decryptDemo(envelope, "user1", Text.class).text());
        verifyNoInteractions(redis);
    }

    @Test
    void enabledReplayRejectsMissingUsedExpiredAndWrongUserChallenges() throws Exception {
        properties.getReplay().setEnabled(true);
        var ready = biz.prepare("CRYPTO_DEMO", "user1");
        assertEquals(43, ready.challengeId().length());
        assertTrue(Long.parseLong(ready.expiresAt()) > System.currentTimeMillis());
        verify(bucket).set(anyString(), eq(120L), eq(TimeUnit.SECONDS));
        var envelope = encrypt(ready, "{\"text\":\"test\"}");
        assertThrows(BuzzException.class, () -> biz.decryptDemo(envelope, "user2", Text.class));
        assertEquals("test", biz.decryptDemo(envelope, "user1", Text.class).text());
        assertThrows(BuzzException.class, () -> biz.decryptDemo(envelope, "user1", Text.class));
        ready = biz.prepare("CRYPTO_DEMO", "user1");
        var expired = encrypt(ready, "{\"text\":\"test\"}");
        stored.set(null); // Redis TTL 到期后的返回行为。
        assertThrows(BuzzException.class, () -> biz.decryptDemo(expired, "user1", Text.class));
        var missing = new CryptoEnvelope(1, "v1", null, "CRYPTO_DEMO", "", "", "");
        assertThrows(BuzzException.class, () -> biz.decryptDemo(missing, "user1", Text.class));
    }

    @Test
    void onlyOneConcurrentRequestCanConsumeAChallenge() throws Exception {
        properties.getReplay().setEnabled(true);
        var envelope = encrypt(biz.prepare("CRYPTO_DEMO", "user1"), "{\"text\":\"test\"}");
        var start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            var futures = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 8; i++) futures.add(pool.submit(() -> {
                start.await();
                try {
                    biz.decryptDemo(envelope, "user1", Text.class);
                    return true;
                } catch (BuzzException e) {
                    return false;
                }
            }));
            start.countDown();
            int successes = 0;
            for (Future<Boolean> future : futures) if (future.get(10, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void rejectsInvalidDataWithoutConsumingChallenge() throws Exception {
        properties.getReplay().setEnabled(true);
        var ready = biz.prepare("CRYPTO_DEMO", "user1");
        for (String body : new String[]{"null", "{}", "{\"text\":null}", "{\"text\":123}", "{\"text\":true}",
                "{\"text\":\"test\",\"password\":\"override\"}", "{\"text\":\"a\",\"text\":\"b\"}", "{\"text\":\"a\"} {}"}) {
            var envelope = encrypt(ready, body);
            assertThrows(BuzzException.class, () -> biz.decryptDemo(envelope, "user1", Text.class), body);
        }
        verify(bucket, never()).getAndDelete();
        assertEquals("", biz.decryptDemo(encrypt(ready, "{\"text\":\"\"}"), "user1", Text.class).text());
    }

    @Test
    void tamperedCiphertextCannotConsumeAValidChallenge() throws Exception {
        properties.getReplay().setEnabled(true);
        var original = encrypt(biz.prepare("CRYPTO_DEMO", "user1"), "{\"text\":\"test\"}");
        byte[] bytes = Base64.getDecoder().decode(original.ciphertext());
        bytes[0] ^= 1;
        var tampered = new CryptoEnvelope(original.v(), original.kid(), original.challengeId(), original.purpose(),
                original.encryptedKey(), original.iv(), Base64.getEncoder().encodeToString(bytes));
        assertThrows(BuzzException.class, () -> biz.decryptDemo(tampered, "user1", Text.class));
        verify(bucket, never()).getAndDelete();
        assertEquals("test", biz.decryptDemo(original, "user1", Text.class).text());
    }

    @Test
    void redisFailuresDoNotDowngradeProtection() throws Exception {
        properties.getReplay().setEnabled(true);
        var ready = biz.prepare("CRYPTO_DEMO", "user1");
        var envelope = encrypt(ready, "{\"text\":\"test\"}");
        when(bucket.getAndDelete()).thenThrow(new IllegalStateException("redis unavailable"));
        assertThrows(BuzzException.class, () -> biz.decryptDemo(envelope, "user1", Text.class));
        when(bucket.get()).thenThrow(new IllegalStateException("redis unavailable"));
        assertThrows(BuzzException.class, () -> biz.decryptDemo(envelope, "user1", Text.class));
    }

    @Test
    void rejectsUnsupportedPurposeAnonymousUsersAndStalePolicy() throws Exception {
        assertThrows(BuzzException.class, () -> biz.prepare("UNKNOWN", "user1"));
        assertThrows(BuzzException.class, () -> biz.prepare("CRYPTO_DEMO", ""));
        properties.getReplay().setEnabled(true);
        var ready = biz.prepare("CRYPTO_DEMO", "user1");
        var envelope = encrypt(ready, "{\"text\":\"test\"}");
        properties.getReplay().setEnabled(false);
        assertThrows(BuzzException.class, () -> biz.decryptDemo(envelope, "user1", Text.class));
    }

    @Test
    void invalidKeyConfigurationFailsWithoutLeakingItsValue() {
        properties.setPrivateKeyFile("sensitive-private-path-that-does-not-exist");
        var error = assertThrows(IllegalStateException.class,
                () -> new RequestCryptoBiz(properties, redis, validators.getValidator(), new ObjectMapper(), new MockEnvironment()));
        assertNull(error.getCause());
        assertFalse(error.getMessage().contains(properties.getPrivateKeyFile()));
    }

    @Test
    void loginPurposesPreserveCredentialsAndRespectBothReplayPolicies() throws Exception {
        for (boolean replay : new boolean[]{false, true}) {
            properties.getReplay().setEnabled(replay);
            for (String purpose : new String[]{RequestCryptoBiz.ADMIN_LOGIN, RequestCryptoBiz.PORTAL_LOGIN}) {
                var ready = biz.prepareLogin(purpose);
                var envelope = encrypt(ready, "{\"username\":\" account \",\"password\":\" 密码🔐 \\n\"}",
                        purpose, loginPath(purpose));
                var credentials = biz.decryptLogin(envelope, purpose);
                assertEquals(" account ", credentials.username());
                assertEquals(" 密码🔐 \n", credentials.password());
                if (replay) {
                    assertThrows(BuzzException.class, () -> biz.decryptLogin(envelope, purpose));
                } else {
                    assertEquals(credentials, biz.decryptLogin(envelope, purpose));
                }
            }
        }
    }

    @Test
    void loginRejectsForeignPurposesRoutesAndInvalidCredentialsBeforeConsumption() throws Exception {
        properties.getReplay().setEnabled(true);
        assertThrows(BuzzException.class, () -> biz.prepareLogin("CRYPTO_DEMO"));
        assertThrows(BuzzException.class, () -> biz.prepareLogin(null));
        var ready = biz.prepareLogin(RequestCryptoBiz.ADMIN_LOGIN);
        var credentials = "{\"username\":\"account\",\"password\":\"test\"}";
        var foreign = encrypt(ready, credentials, RequestCryptoBiz.PORTAL_LOGIN, RequestCryptoBiz.PORTAL_LOGIN_PATH);
        assertThrows(BuzzException.class, () -> biz.decryptLogin(foreign, RequestCryptoBiz.PORTAL_LOGIN));
        var wrongRoute = encrypt(ready, credentials, RequestCryptoBiz.ADMIN_LOGIN, RequestCryptoBiz.PORTAL_LOGIN_PATH);
        assertThrows(BuzzException.class, () -> biz.decryptLogin(wrongRoute, RequestCryptoBiz.ADMIN_LOGIN));
        var original = encrypt(ready, credentials, RequestCryptoBiz.ADMIN_LOGIN, RequestCryptoBiz.ADMIN_LOGIN_PATH);
        assertThrows(BuzzException.class, () -> biz.decryptLogin(original, RequestCryptoBiz.PORTAL_LOGIN));
        assertThrows(BuzzException.class, () -> biz.decryptDemo(original, "user1", Text.class));
        for (String body : new String[]{"{}", "null", "{\"username\":\"a\",\"password\":\" \"}",
                "{\"username\":123,\"password\":\"a\"}", "{\"username\":\"a\",\"password\":\"a\",\"extra\":true}",
                "{\"username\":\"a\",\"password\":\"a\",\"password\":\"b\"}"}) {
            var invalid = encrypt(ready, body, RequestCryptoBiz.ADMIN_LOGIN, RequestCryptoBiz.ADMIN_LOGIN_PATH);
            assertThrows(BuzzException.class, () -> biz.decryptLogin(invalid, RequestCryptoBiz.ADMIN_LOGIN));
        }
        verify(bucket, never()).getAndDelete();
        assertEquals("account", biz.decryptLogin(original, RequestCryptoBiz.ADMIN_LOGIN).username());
    }

    @Test
    void loginRejectsExpiredChallengesAndRedisFailureWithoutFallback() throws Exception {
        properties.getReplay().setEnabled(true);
        var ready = biz.prepareLogin(RequestCryptoBiz.PORTAL_LOGIN);
        var envelope = encrypt(ready, "{\"username\":\"account\",\"password\":\"test\"}",
                RequestCryptoBiz.PORTAL_LOGIN, RequestCryptoBiz.PORTAL_LOGIN_PATH);
        stored.set(null);
        assertThrows(BuzzException.class, () -> biz.decryptLogin(envelope, RequestCryptoBiz.PORTAL_LOGIN));
        when(bucket.get()).thenThrow(new IllegalStateException("redis unavailable"));
        assertThrows(BuzzException.class, () -> biz.decryptLogin(envelope, RequestCryptoBiz.PORTAL_LOGIN));
        verify(bucket, never()).getAndDelete();
    }

    private String loginPath(String purpose) {
        return RequestCryptoBiz.ADMIN_LOGIN.equals(purpose) ? RequestCryptoBiz.ADMIN_LOGIN_PATH : RequestCryptoBiz.PORTAL_LOGIN_PATH;
    }

    @Test
    void failedAuthenticationStillConsumesTheChallengeBeforeCallingExistingAuth() throws Exception {
        properties.getReplay().setEnabled(true);
        var auth = mock(AuthBiz.class);
        when(auth.login(any())).thenThrow(new BuzzException("账号或密码错误"));
        when(auth.portalLoginWithTrust(any())).thenThrow(new BuzzException("账号或密码错误"));
        var controller = new RequestCryptoLoginController(biz, auth, mock(UserBiz.class));
        for (String purpose : new String[]{RequestCryptoBiz.ADMIN_LOGIN, RequestCryptoBiz.PORTAL_LOGIN}) {
            var envelope = encrypt(biz.prepareLogin(purpose), "{\"username\":\"account\",\"password\":\"wrong\"}", purpose, loginPath(purpose));
            org.junit.jupiter.api.function.Executable submit = () -> {
                if (RequestCryptoBiz.ADMIN_LOGIN.equals(purpose)) controller.adminLogin(envelope);
                else controller.portalLogin(envelope);
            };
            assertEquals("账号或密码错误", assertThrows(BuzzException.class, submit).getMessage());
            assertEquals("挑战已过期、已使用或不匹配", assertThrows(BuzzException.class, submit).getMessage());
        }
        verify(auth, times(1)).login(any());
        verify(auth, times(1)).portalLoginWithTrust(any());
    }

    private CryptoEnvelope encrypt(RequestCryptoBiz.Preparation ready, String plaintext) throws Exception {
        return encrypt(ready, plaintext, RequestCryptoBiz.DEMO_PURPOSE, RequestCryptoBiz.DEMO_PATH);
    }

    private CryptoEnvelope encrypt(RequestCryptoBiz.Preparation ready, String plaintext, String purpose, String path) throws Exception {
        var keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        var key = keyGenerator.generateKey();
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        var rsa = Cipher.getInstance("RSA/ECB/OAEPPadding");
        rsa.init(Cipher.ENCRYPT_MODE, pair.getPublic(),
                new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
        var envelope = new CryptoEnvelope(1, ready.kid(), ready.challengeId(), purpose,
                Base64.getEncoder().encodeToString(rsa.doFinal(key.getEncoded())), Base64.getEncoder().encodeToString(iv), null);
        var aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
        aes.updateAAD(RequestCrypto.aad(envelope, "POST", path));
        return new CryptoEnvelope(1, ready.kid(), ready.challengeId(), envelope.purpose(), envelope.encryptedKey(), envelope.iv(),
                Base64.getEncoder().encodeToString(aes.doFinal(plaintext.getBytes(StandardCharsets.UTF_8))));
    }

    public record Text(@NotNull String text) {}
}
