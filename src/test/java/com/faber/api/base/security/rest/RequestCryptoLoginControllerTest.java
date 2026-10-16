package com.faber.api.base.security.rest;

import cn.dev33.satoken.stp.SaTokenInfo;
import com.faber.api.base.admin.biz.AuthBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.security.biz.RequestCryptoBiz;
import com.faber.config.interceptor.LicenseGuardInterceptor;
import com.faber.config.interceptor.PermissionInterceptor;
import com.faber.config.interceptor.UserAuthRestInterceptor;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.crypto.CryptoEnvelope;
import com.faber.core.license.LicenseManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RequestCryptoLoginControllerTest {
    private final RequestCryptoBiz crypto = mock(RequestCryptoBiz.class);
    private final AuthBiz auth = mock(AuthBiz.class);
    private final UserBiz users = mock(UserBiz.class);
    private final RequestCryptoLoginController controller = new RequestCryptoLoginController(crypto, auth, users);
    private final CryptoEnvelope envelope = new CryptoEnvelope(1, "test", null, "ADMIN_LOGIN", "test", "test", "test");

    @AfterEach
    void clearContext() { BaseContextHandler.remove(); }

    @Test
    void preservesExistingAdminAndPortalResponsesAndExactCredentials() {
        var credentials = new RequestCryptoBiz.LoginCredentials("account", " password ");
        when(crypto.decryptLogin(any(), anyString())).thenReturn(credentials);
        var token = new SaTokenInfo();
        token.tokenValue = "test-session";
        when(auth.login(argThat(value -> value.getPassword().equals(" password ")))).thenReturn(token);
        assertSame(token, controller.adminLogin(envelope).getData());
        BaseContextHandler.setUserId("user1");
        var user = new User();
        user.setId("user1");
        user.setUsername("account");
        when(users.getById("user1")).thenReturn(user);
        when(auth.portalLoginWithTrust(argThat(value -> value.getUsername().equals("account")
                && value.getPassword().equals(" password ")))).thenReturn(new AuthBiz.PortalLoginResult(token, "test-trust"));
        var session = controller.portalLogin(envelope).getData();
        assertEquals("test-session", session.getToken());
        assertEquals("test-trust", session.getDeviceTrustToken());
        assertEquals("account", session.getUser().getUsername());
        verify(crypto).decryptLogin(envelope, RequestCryptoBiz.ADMIN_LOGIN);
        verify(crypto).decryptLogin(envelope, RequestCryptoBiz.PORTAL_LOGIN);
    }

    @Test
    void loginAndPreparationAreAnonymousAndLicenseIndependentButRejectPlaintextAndExtraFields() throws Exception {
        var license = mock(LicenseManager.class);
        var mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        var mvc = MockMvcBuilders.standaloneSetup(controller, new RequestCryptoController(crypto))
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .addInterceptors(new UserAuthRestInterceptor(), new PermissionInterceptor(), new LicenseGuardInterceptor(license))
                .build();
        mvc.perform(post("/api/base/security/crypto/prepare-login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"purpose\":\"ADMIN_LOGIN\"}")).andExpect(status().isOk());
        verify(crypto).prepareLogin("ADMIN_LOGIN");
        for (String path : new String[]{RequestCryptoBiz.ADMIN_LOGIN_PATH, RequestCryptoBiz.PORTAL_LOGIN_PATH}) {
            for (String body : new String[]{"{\"username\":\"account\",\"password\":\"test\"}",
                    mapper.writeValueAsString(envelope).replaceFirst("\\}$", ",\"password\":\"override\"}")}) {
                mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
            }
        }
        try (var stp = mockStatic(cn.dev33.satoken.stp.StpUtil.class)) {
            stp.when(cn.dev33.satoken.stp.StpUtil::getTokenValue).thenReturn("");
            mvc.perform(post("/api/base/security/crypto/prepare").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"purpose\":\"CRYPTO_DEMO\"}")).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(auth, users, license);
        verify(crypto, never()).decryptLogin(any(), anyString());
    }
}
