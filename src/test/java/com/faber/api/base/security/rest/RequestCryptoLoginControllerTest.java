package com.faber.api.base.security.rest;

import cn.dev33.satoken.stp.SaTokenInfo;
import com.faber.api.base.admin.biz.AuthBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.rbac.biz.RbacUserRoleBiz;
import com.faber.api.base.security.biz.RequestCryptoBiz;
import com.faber.api.base.security.config.RequestCryptoProperties;
import com.faber.config.interceptor.*;
import com.faber.config.utils.user.LoginReqVo;
import com.faber.core.config.exception.GlobalExceptionHandler;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.auth.UserNoPermissionException;
import com.faber.core.license.LicenseManager;
import com.faber.core.utils.FaRedisUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static com.faber.api.base.security.FieldCryptoTestSupport.*;
import static com.faber.api.base.security.biz.RequestCryptoBiz.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RequestCryptoLoginControllerTest {
    private final AuthBiz auth = mock(AuthBiz.class);
    private final UserBiz users = mock(UserBiz.class);
    private final RbacUserRoleBiz rbac = mock(RbacUserRoleBiz.class);
    private final LicenseManager license = mock(LicenseManager.class);
    private RequestCryptoBiz crypto;
    private RequestCryptoController security;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        var properties = new RequestCryptoProperties();
        properties.setPrivateKeyFile("classpath:crypto/dev-public-request-key.pem");
        var env = new MockEnvironment(); env.setActiveProfiles("dev");
        crypto = new RequestCryptoBiz(properties, mock(FaRedisUtils.class), env);
        security = new RequestCryptoController(crypto, rbac);
        mvc = MockMvcBuilders.standaloneSetup(new RequestCryptoLoginController(crypto, auth, users), security)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addInterceptors(new UserAuthRestInterceptor(), new PermissionInterceptor(), new LicenseGuardInterceptor(license)).build();
    }
    @AfterEach void clearContext() { BaseContextHandler.remove(); }

    @Test
    void headerBasedLoginPreservesAdminAndPortalResultsAndExactPassword() throws Exception {
        var token = new SaTokenInfo(); token.tokenValue = "test-session";
        when(auth.login(any())).thenReturn(token);
        when(auth.portalLoginWithTrust(any())).thenAnswer(call -> {
            BaseContextHandler.setUserId("user1"); return new AuthBiz.PortalLoginResult(token, "test-trust");
        });
        var user = new User(); user.setId("user1"); user.setUsername("account");
        when(users.getById("user1")).thenReturn(user);
        for (String path : new String[]{ADMIN_LOGIN_PATH, PORTAL_LOGIN_PATH}) {
            var ready = crypto.prepare(path, "POST", null, true);
            var body = new LoginReqVo("account", encrypt(crypto, ready, path, "password", " password "));
            var response = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                    .header("FaCryptoVersion", "2").header("FaCryptoKid", ready.kid())
                    .content(new ObjectMapper().writeValueAsString(body))).andExpect(status().isOk());
            if (path.equals(ADMIN_LOGIN_PATH)) response.andExpect(jsonPath("$.data.tokenValue").value("test-session"));
            else response.andExpect(jsonPath("$.data.token").value("test-session"))
                    .andExpect(jsonPath("$.data.deviceTrustToken").value("test-trust"));
        }
        var captured = ArgumentCaptor.forClass(LoginReqVo.class);
        verify(auth).login(captured.capture()); assertEquals(" password ", captured.getValue().getPassword());
        verify(auth).portalLoginWithTrust(captured.capture()); assertEquals(" password ", captured.getValue().getPassword());
        verifyNoInteractions(license);
    }

    @Test
    void plaintextAndMissingHeadersFailWithoutAuthenticatingOrFallingBack() throws Exception {
        for (String path : new String[]{ADMIN_LOGIN_PATH, PORTAL_LOGIN_PATH}) {
            var ready = crypto.prepare(path, "POST", null, true);
            var cipher = encrypt(crypto, ready, path, "password", "test");
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                    .content(new ObjectMapper().writeValueAsString(new LoginReqVo("account", cipher))))
                    .andExpect(jsonPath("$.message").value("字段加密协议不支持"));
            mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).header("FaCryptoVersion", "2")
                    .header("FaCryptoKid", ready.kid()).content("{\"username\":\"account\",\"password\":\"plaintext\"}"))
                    .andExpect(jsonPath("$.message").value("字段密文无效"));
        }
        verifyNoInteractions(auth, users);
    }

    @Test
    void anonymousPreparationOnlyAllowsLoginWhileBusinessNeedsAuthenticationAndPermission() throws Exception {
        mvc.perform(post("/api/base/security/crypto/prepare-login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"POST\",\"path\":\"/api/base/admin/auth/loginV2\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.v").value(2))
                .andExpect(jsonPath("$.data.fields[0]").value("password"));
        mvc.perform(post("/api/base/security/crypto/prepare-login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"method\":\"POST\",\"path\":\"/api/base/demo/crypto/roundtrip\"}"))
                .andExpect(jsonPath("$.message").value("不支持的加密目标"));
        try (var stp = mockStatic(cn.dev33.satoken.stp.StpUtil.class)) {
            stp.when(cn.dev33.satoken.stp.StpUtil::getTokenValue).thenReturn("");
            mvc.perform(post("/api/base/security/crypto/prepare").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"method\":\"POST\",\"path\":\"/api/base/demo/crypto/roundtrip\"}"))
                    .andExpect(status().isUnauthorized());
        }
        BaseContextHandler.setUserId("user1");
        var body = new RequestCryptoController.PrepareRequest(DEMO_PATH, "POST");
        assertThrows(UserNoPermissionException.class, () -> security.prepare(body));
        when(rbac.checkUserLinkUrl("user1", DEMO_PERMISSION)).thenReturn(true);
        assertEquals("text", security.prepare(body).getData().fields().get(0));
    }
}
