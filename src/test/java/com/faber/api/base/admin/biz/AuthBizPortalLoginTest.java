package com.faber.api.base.admin.biz;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.faber.api.base.admin.entity.LogLogin;
import com.faber.api.base.admin.entity.User;
import com.faber.config.utils.user.LoginReqVo;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.IpUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthBizPortalLoginTest {
    private final AuthBiz auth = new AuthBiz();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private MockedStatic<StpUtil> stp;
    private MockedStatic<IpUtils> ip;

    @BeforeEach
    void setup() {
        auth.userBiz = mock(UserBiz.class);
        auth.smsCodeBiz = mock(SmsCodeBiz.class);
        auth.userDeviceBiz = mock(UserDeviceBiz.class);
        auth.logLoginBiz = mock(LogLoginBiz.class);
        User user = new User();
        user.setId("portal-user");
        user.setStatus(true);
        when(auth.userBiz.validate("user", "password")).thenReturn(user);
        when(auth.userBiz.getUserByTel("phone")).thenReturn(user);
        when(auth.smsCodeBiz.consumeLoginCode("phone", "code")).thenReturn(true);
        request.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        stp = mockStatic(StpUtil.class);
        stp.when(StpUtil::getTokenInfo).thenReturn(new SaTokenInfo());
        ip = mockStatic(IpUtils.class);
    }

    @AfterEach
    void cleanup() {
        ip.close();
        stp.close();
        RequestContextHolder.resetRequestAttributes();
        BaseContextHandler.remove();
    }

    @ParameterizedTest
    @CsvSource({"WEB, WEB", "desktop, DESKTOP", "invalid, WEB"})
    void browserAndDesktopLoginRespectClientType(String header, String expected) {
        request.addHeader("X-Telemetry-Client-Type", header);
        request.addHeader("FaClientInstanceId", "client-id");
        // 显式客户端类型优先于旧版来源标识。
        request.addHeader("FaFrom", "FaWeb");
        assertNull(passwordLogin().deviceTrustToken());
        verify(auth.userDeviceBiz).registerClientOnLogin(any(), eq(expected), eq("client-id"),
                isNull(), isNull(), anyString(), isNull());
        assertLoggedClientType(expected);
    }

    @Test
    void portalWithoutDeviceHeadersCanLoginByPasswordAndSms() {
        assertNull(passwordLogin().deviceTrustToken());
        assertNull(auth.portalLoginBySms("phone", "code").deviceTrustToken());
        verifyNoInteractions(auth.userDeviceBiz);
        var logs = ArgumentCaptor.forClass(LogLogin.class);
        verify(auth.logLoginBiz, times(2)).save(logs.capture());
        assertTrue(logs.getAllValues().stream().allMatch(log -> "WEB".equals(log.getClientType())));
        stp.verify(() -> StpUtil.login("portal-user", "portal"), times(2));
    }

    @ParameterizedTest
    @CsvSource({"X-Telemetry-Client-Type, MOBILE", "FaFrom, FaApp"})
    void mobileStillRequiresDeviceIdentity(String header, String value) {
        request.addHeader(header, value);
        assertThrows(BuzzException.class, this::passwordLogin);
        verifyNoInteractions(auth.userDeviceBiz, auth.logLoginBiz);
        stp.verify(() -> StpUtil.login(any(), anyString()), never());
    }

    @Test
    void mobileTrustIsCreatedBeforeSessionAndFailureBlocksLogin() {
        request.addHeader("X-Telemetry-Client-Type", "MOBILE");
        request.addHeader("FaClientInstanceId", "mobile-id");
        request.addHeader("FaDeviceTrustToken", "previous-trust");
        when(auth.userDeviceBiz.trustClientOnPasswordLogin(any(), eq("MOBILE"), eq("mobile-id"),
                isNull(), isNull(), anyString(), isNull(), eq("previous-trust")))
                .thenAnswer(invocation -> {
                    stp.verify(() -> StpUtil.login(any(), anyString()), never());
                    return "new-trust";
                });
        assertEquals("new-trust", passwordLogin().deviceTrustToken());
        assertLoggedClientType("MOBILE");
        stp.clearInvocations();
        when(auth.userDeviceBiz.trustClientOnPasswordLogin(any(), eq("MOBILE"), eq("mobile-id"),
                isNull(), isNull(), anyString(), isNull(), eq("previous-trust")))
                .thenThrow(new BuzzException("设备不受信任"));
        assertThrows(BuzzException.class, this::passwordLogin);
        stp.verify(() -> StpUtil.login(any(), anyString()), never());
    }

    private AuthBiz.PortalLoginResult passwordLogin() {
        return auth.portalLoginWithTrust(new LoginReqVo("user", "password"));
    }

    private void assertLoggedClientType(String expected) {
        var log = ArgumentCaptor.forClass(LogLogin.class);
        verify(auth.logLoginBiz).save(log.capture());
        assertEquals(expected, log.getValue().getClientType());
        stp.verify(() -> StpUtil.login("portal-user", "portal"));
    }
}
