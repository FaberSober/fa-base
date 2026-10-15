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
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        Path file = directory.resolve("test-only.key");
        Files.writeString(file, Base64.getEncoder().encodeToString(generator.generateKeyPair().getPrivate().getEncoded()));
        context().withPropertyValues("fa.security.crypto.enabled=true", "fa.security.crypto.kid=config-test",
                        "fa.security.crypto.private-key-file=" + file, "fa.security.crypto.replay.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RequestCryptoBiz.class).hasSingleBean(RequestCryptoController.class);
                    assertThat(context.getBean(RequestCryptoBiz.class).publicKey().kid()).isEqualTo("config-test");
                    assertThat(context.getBean(RequestCryptoBiz.class).prepare("CRYPTO_DEMO", "user1").replayEnabled()).isFalse();
                });
    }
}
