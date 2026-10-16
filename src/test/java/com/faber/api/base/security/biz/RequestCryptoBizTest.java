package com.faber.api.base.security.biz;

import com.faber.api.base.security.config.RequestCryptoProperties;
import com.faber.api.base.security.rest.RequestCryptoLoginController;
import com.faber.api.base.admin.biz.AuthBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.config.utils.user.LoginReqVo;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaRedisUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.mock.env.MockEnvironment;
import java.nio.file.*;
import java.security.KeyPairGenerator;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static com.faber.api.base.security.FieldCryptoTestSupport.*;
import static com.faber.api.base.security.biz.RequestCryptoBiz.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RequestCryptoBizTest {
    @TempDir Path directory;
    private final RequestCryptoProperties properties = new RequestCryptoProperties();
    private final FaRedisUtils redis = mock(FaRedisUtils.class);
    private final RedissonClient redisson = mock(RedissonClient.class);
    private final Map<String, String> stored = new ConcurrentHashMap<>();
    private final AtomicInteger consumed = new AtomicInteger();
    private boolean redisFailure;
    private RequestCryptoBiz biz;

    @BeforeEach
    void setup() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048);
        Path key = directory.resolve("test-only.pem");
        Files.writeString(key, Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded()));
        properties.setPrivateKeyFile(key.toString());
        when(redis.getRedisson()).thenReturn(redisson);
        when(redis.buildKey(anyString())).thenAnswer(call -> call.getArgument(0));
        when(redisson.<String>getBucket(anyString())).thenAnswer(call -> {
            if (redisFailure) throw new IllegalStateException("unavailable");
            String id = call.getArgument(0);
            @SuppressWarnings("unchecked") RBucket<String> bucket = mock(RBucket.class);
            doAnswer(value -> { stored.put(id, value.getArgument(0)); return null; }).when(bucket).set(anyString(), eq(120L), eq(TimeUnit.SECONDS));
            when(bucket.get()).thenAnswer(value -> stored.get(id));
            when(bucket.getAndDelete()).thenAnswer(value -> { consumed.incrementAndGet(); return stored.remove(id); });
            return bucket;
        });
        biz = new RequestCryptoBiz(properties, redis, new MockEnvironment());
        BaseContextHandler.setUserId("user1");
    }

    @AfterEach void clearContext() { BaseContextHandler.remove(); }

    @Test
    void disabledReplayPreservesFieldStringsAndNeverUsesRedis() throws Exception {
        for (String path : List.of(DEMO_PATH, ADMIN_LOGIN_PATH, PORTAL_LOGIN_PATH)) {
            var target = biz.target(path, "POST");
            var ready = biz.prepare(path, "POST", target.login() ? null : "user1", target.login());
            assertEquals(2, ready.v()); assertNull(ready.challengeId()); assertNull(ready.expiresAt());
            assertEquals(List.of(target.field()), ready.fields());
            for (String value : List.of("", " 中文🔐 \n", "a".repeat(4096))) {
                String cipher = encrypt(biz, ready, path, target.field(), value);
                assertEquals(value, biz.decryptField(cipher, request(ready, path), target.field()));
                assertEquals(value, biz.decryptField(cipher, request(ready, path), target.field()));
            }
        }
        verifyNoInteractions(redis);
    }

    @Test
    void enabledReplayBindsUserAndRejectsUsedExpiredAndMissingChallenges() throws Exception {
        properties.getReplay().setEnabled(true);
        var ready = biz.prepare(DEMO_PATH, "POST", "user1", false);
        assertEquals(43, ready.challengeId().length());
        assertTrue(Long.parseLong(ready.expiresAt()) > System.currentTimeMillis());
        String cipher = encrypt(biz, ready, DEMO_PATH, "text", "test");
        BaseContextHandler.setUserId("user2");
        assertThrows(BuzzException.class, () -> biz.decryptField(cipher, request(ready, DEMO_PATH), "text"));
        BaseContextHandler.setUserId("user1");
        assertEquals("test", biz.decryptField(cipher, request(ready, DEMO_PATH), "text"));
        assertThrows(BuzzException.class, () -> biz.decryptField(cipher, request(ready, DEMO_PATH), "text"));
        var expired = biz.prepare(DEMO_PATH, "POST", "user1", false);
        stored.clear();
        assertThrows(BuzzException.class, () -> biz.decryptField(cipher, request(expired, DEMO_PATH), "text"));
        var missing = request(ready, DEMO_PATH); missing.removeHeader("FaCryptoChallengeId");
        assertThrows(BuzzException.class, () -> biz.decryptField(cipher, missing, "text"));
    }

    @Test
    void onlyOneConcurrentSubmissionCanConsumeAChallenge() throws Exception {
        properties.getReplay().setEnabled(true);
        var ready = biz.prepare(ADMIN_LOGIN_PATH, "POST", null, true);
        String cipher = encrypt(biz, ready, ADMIN_LOGIN_PATH, "password", "test");
        var start = new CountDownLatch(1); var pool = Executors.newFixedThreadPool(8);
        try {
            var tasks = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < 8; i++) tasks.add(pool.submit(() -> {
                start.await();
                try { biz.decryptField(cipher, request(ready, ADMIN_LOGIN_PATH), "password"); return true; }
                catch (BuzzException e) { return false; }
            }));
            start.countDown(); int successes = 0;
            for (var task : tasks) if (task.get(10, TimeUnit.SECONDS)) successes++;
            assertEquals(1, successes);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void independentRequestsNeverShareChallenges() throws Exception {
        properties.getReplay().setEnabled(true);
        var a = biz.prepare(ADMIN_LOGIN_PATH, "POST", null, true);
        var b = biz.prepare(ADMIN_LOGIN_PATH, "POST", null, true);
        assertNotEquals(a.challengeId(), b.challengeId());
        for (var ready : List.of(a, b)) {
            String cipher = encrypt(biz, ready, ADMIN_LOGIN_PATH, "password", "test");
            assertEquals("test", biz.decryptField(cipher, request(ready, ADMIN_LOGIN_PATH), "password"));
        }
    }

    @Test
    void rejectsTamperingHeaderSubstitutionAndWrongFieldsWithoutConsumption() throws Exception {
        properties.getReplay().setEnabled(true);
        var ready = biz.prepare(ADMIN_LOGIN_PATH, "POST", null, true);
        String cipher = encrypt(biz, ready, ADMIN_LOGIN_PATH, "password", "test");
        for (String header : List.of("FaCryptoVersion", "FaCryptoKid", "FaCryptoChallengeId")) {
            var missing = request(ready, ADMIN_LOGIN_PATH); missing.removeHeader(header);
            assertThrows(BuzzException.class, () -> biz.decryptField(cipher, missing, "password"));
            var duplicate = request(ready, ADMIN_LOGIN_PATH); duplicate.addHeader(header, "other");
            assertThrows(BuzzException.class, () -> biz.decryptField(cipher, duplicate, "password"));
        }
        String[] parts = cipher.split("\\."); parts[2] = (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        assertThrows(BuzzException.class, () -> biz.decryptField(String.join(".", parts), request(ready, ADMIN_LOGIN_PATH), "password"));
        var other = biz.prepare(ADMIN_LOGIN_PATH, "POST", null, true);
        assertThrows(BuzzException.class, () -> biz.decryptField(cipher, request(other, ADMIN_LOGIN_PATH), "password"));
        assertThrows(BuzzException.class, () -> biz.decryptField(cipher, request(ready, PORTAL_LOGIN_PATH), "password"));
        assertThrows(BuzzException.class, () -> biz.decryptField(cipher, request(ready, ADMIN_LOGIN_PATH), "text"));
        assertEquals(0, consumed.get());
        assertEquals("test", biz.decryptField(cipher, request(ready, ADMIN_LOGIN_PATH), "password"));
    }

    @Test
    void rejectsPlaintextOversizeAndStalePolicy() throws Exception {
        var ready = biz.prepare(ADMIN_LOGIN_PATH, "POST", null, true);
        for (String value : List.of("password", "a.b.c", "x".repeat(6000))) {
            assertThrows(BuzzException.class, () -> biz.decryptField(value, request(ready, ADMIN_LOGIN_PATH), "password"));
        }
        properties.getReplay().setEnabled(true);
        var protectedReady = biz.prepare(ADMIN_LOGIN_PATH, "POST", null, true);
        var cipher = encrypt(biz, protectedReady, ADMIN_LOGIN_PATH, "password", "test");
        properties.getReplay().setEnabled(false);
        assertThrows(BuzzException.class, () -> biz.decryptField(cipher, request(protectedReady, ADMIN_LOGIN_PATH), "password"));
    }

    @Test
    void failedLoginStillConsumesChallengeBeforeExistingAuthentication() throws Exception {
        properties.getReplay().setEnabled(true);
        var auth = mock(AuthBiz.class);
        when(auth.login(any())).thenThrow(new BuzzException("账号或密码错误"));
        when(auth.portalLoginWithTrust(any())).thenThrow(new BuzzException("账号或密码错误"));
        var controller = new RequestCryptoLoginController(biz, auth, mock(UserBiz.class));
        for (String path : List.of(ADMIN_LOGIN_PATH, PORTAL_LOGIN_PATH)) {
            var ready = biz.prepare(path, "POST", null, true);
            String cipher = encrypt(biz, ready, path, "password", "wrong");
            org.junit.jupiter.api.function.Executable submit = () -> {
                var body = new LoginReqVo("account", cipher);
                if (path.equals(ADMIN_LOGIN_PATH)) controller.adminLogin(body, request(ready, path));
                else controller.portalLogin(body, request(ready, path));
            };
            assertEquals("账号或密码错误", assertThrows(BuzzException.class, submit).getMessage());
            assertEquals("挑战已过期、已使用或不匹配", assertThrows(BuzzException.class, submit).getMessage());
        }
        verify(auth).login(any()); verify(auth).portalLoginWithTrust(any());
    }

    @Test
    void rejectsUnregisteredTargetsAndAnonymousBusinessPreparation() {
        assertThrows(BuzzException.class, () -> biz.prepare(DEMO_PATH, "POST", null, false));
        assertThrows(BuzzException.class, () -> biz.prepare(DEMO_PATH, "POST", null, true));
        assertThrows(BuzzException.class, () -> biz.prepare(ADMIN_LOGIN_PATH, "GET", null, true));
        assertThrows(BuzzException.class, () -> biz.prepare("/api/other", "POST", null, true));
    }

    @Test
    void redisFailureNeverDowngradesToPlaintextOrProcessLocalReplayState() throws Exception {
        properties.getReplay().setEnabled(true);
        var ready = biz.prepare(ADMIN_LOGIN_PATH, "POST", null, true);
        String cipher = encrypt(biz, ready, ADMIN_LOGIN_PATH, "password", "test");
        redisFailure = true;
        assertThrows(BuzzException.class, () -> biz.prepare(ADMIN_LOGIN_PATH, "POST", null, true));
        assertThrows(BuzzException.class, () -> biz.decryptField(cipher, request(ready, ADMIN_LOGIN_PATH), "password"));
    }

    @Test
    void invalidKeyConfigurationNeverLeaksItsValue() {
        properties.setPrivateKeyFile("sensitive-private-path");
        var error = assertThrows(IllegalStateException.class, () -> new RequestCryptoBiz(properties, redis, new MockEnvironment()));
        assertNull(error.getCause()); assertFalse(error.getMessage().contains("sensitive-private-path"));
    }
}
