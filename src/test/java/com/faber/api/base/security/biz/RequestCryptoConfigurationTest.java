package com.faber.api.base.security.biz;

import com.faber.api.base.security.config.RequestCryptoProperties;
import com.faber.api.base.security.rest.RequestCryptoController;
import com.faber.core.utils.FaRedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RequestCryptoConfigurationTest {

    @TempDir Path directory;
    private static ValidatorFactory validators;

    @BeforeAll
    static void validator() {
        validators = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validators.close();
    }

    private ApplicationContextRunner context() {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(ConfigurationPropertiesAutoConfiguration.class))
                .withUserConfiguration(RequestCryptoProperties.class, RequestCryptoBiz.class, RequestCryptoController.class)
                .withPropertyValues("spring.data.redis.prefix=crypto-test")
                .withBean(FaRedisUtils.class, () -> mock(FaRedisUtils.class))
                .withBean(RedissonClient.class, () -> mock(RedissonClient.class))
                .withBean(ObjectMapper.class, ObjectMapper::new)
                .withBean(Validator.class, validators::getValidator);
    }

    private ApplicationContextRunner application(String profile) {
        Path config = Path.of(System.getProperty("basedir"), "..", "fa-admin", "src", "main", "resources", "application.yml")
                .normalize();
        return context().withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.location=" + config.toUri(), "spring.profiles.active=" + profile);
    }

    private Path externalKey() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        Path file = directory.resolve("test-only.key");
        Files.writeString(file, Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded()));
        return file;
    }

    @Test
    void defaultDisabledFeatureDoesNotRequireAKeyOrExposeEndpoints() {
        context().run(context -> assertThat(context).hasNotFailed()
                .doesNotHaveBean(RequestCryptoBiz.class).doesNotHaveBean(RequestCryptoController.class));
    }

    @Test
    void explicitlyEnabledFeatureFailsStartupWithoutAValidKey() {
        context().withPropertyValues("fa.security.crypto.enabled=true")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasStackTraceContaining("请求加密配置无效");
                });
    }

    @Test
    void validExternalKeyBindsConfigurationAndEnablesTheController() throws Exception {
        Path file = externalKey();
        context().withPropertyValues("fa.security.crypto.enabled=true", "fa.security.crypto.kid=config-test",
                        "fa.security.crypto.private-key-file=" + file, "fa.security.crypto.replay.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RequestCryptoBiz.class).hasSingleBean(RequestCryptoController.class);
                    assertThat(context.getBean(RequestCryptoBiz.class).publicKey().kid()).isEqualTo("config-test");
                    assertThat(context.getBean(RequestCryptoBiz.class).prepare("CRYPTO_DEMO", "user1").replayEnabled()).isFalse();
                });
    }

    @Test
    void devDefaultsLoadTheBundledPublicTestKeyUsingActualApplicationYaml() {
        application("dev").run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RequestCryptoBiz.class);
            assertThat(context.getBean(RequestCryptoProperties.class).getPrivateKeyFile())
                    .isEqualTo("classpath:crypto/dev-public-request-key.pem");
            assertThat(context.getBean(RequestCryptoBiz.class).publicKey().kid()).isEqualTo("dev-public-v1");
        });
    }

    @Test
    void devCanOverrideThePublicTestKeyWithAnExternalKey() throws Exception {
        Path file = externalKey();
        application("dev").withPropertyValues("FA_CRYPTO_PRIVATE_KEY_FILE=" + file, "FA_CRYPTO_KID=dev-external")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RequestCryptoProperties.class).getPrivateKeyFile()).isEqualTo(file.toString());
                    assertThat(context.getBean(RequestCryptoBiz.class).publicKey().kid()).isEqualTo("dev-external");
                });
    }

    @Test
    void prodCannotFallBackToTheBundledKeyWhenTheExternalKeyIsMissing() {
        application("prod").withPropertyValues("FA_CRYPTO_PRIVATE_KEY_FILE=" + directory.resolve("missing.key"))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasStackTraceContaining("请求加密配置无效");
                });
    }

    @Test
    void prodCanLoadItsOwnExternalKey() throws Exception {
        application("prod").withPropertyValues("FA_CRYPTO_PRIVATE_KEY_FILE=" + externalKey(), "FA_CRYPTO_KID=prod-external")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(RequestCryptoBiz.class).publicKey().kid()).isEqualTo("prod-external");
                });
    }

    @Test
    void classpathKeysAreRejectedOutsideDevAndWhenProdIsAlsoActive() {
        for (String profile : new String[]{"prod", "dev,prod", "test"}) {
            context().withPropertyValues("spring.profiles.active=" + profile, "fa.security.crypto.enabled=true",
                            "fa.security.crypto.private-key-file=classpath:crypto/dev-public-request-key.pem")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class)
                                .hasStackTraceContaining("classpath 测试密钥仅允许 dev 环境");
                    });
        }
    }
}
