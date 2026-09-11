package com.faber.config.interceptor;

import com.faber.core.exception.license.LicenseInvalidException;
import com.faber.core.license.LicenseManager;
import com.faber.core.license.LicenseState;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LicenseGuardInterceptorTest {

    private final LicenseManager licenseManager = mock(LicenseManager.class);
    private final LicenseGuardInterceptor interceptor = new LicenseGuardInterceptor(licenseManager);

    @Test
    void allowsUsableStates() throws Exception {
        HandlerMethod handler = handler();
        for (LicenseState state : List.of(LicenseState.ACTIVE, LicenseState.GRACE, LicenseState.BYPASSED)) {
            when(licenseManager.getState()).thenReturn(state);
            assertTrue(interceptor.preHandle(request("GET", "/api/base/admin/user/page"),
                    new MockHttpServletResponse(), handler));
        }
    }

    @Test
    void rejectsUnusableStates() throws Exception {
        HandlerMethod handler = handler();
        for (LicenseState state : List.of(
                LicenseState.EXPIRED,
                LicenseState.DISABLED,
                LicenseState.MACHINE_MISMATCH,
                LicenseState.INVALID,
                LicenseState.TIME_ANOMALY,
                LicenseState.BLOCKED,
                LicenseState.UNCONFIGURED)) {
            when(licenseManager.getState()).thenReturn(state);
            assertThrows(LicenseInvalidException.class, () -> interceptor.preHandle(
                    request("GET", "/api/base/admin/user/page"), new MockHttpServletResponse(), handler));
        }
    }

    @Test
    void skipsWhitelistedAndNonApiRequests() throws Exception {
        HandlerMethod handler = handler();
        for (String uri : List.of(
                "/api/base/admin/auth/login",
                "/api/base/admin/auth/loginByToken",
                "/api/portal/auth/login",
                "/api/portal/auth/register",
                "/api/base/admin/license/info",
                "/api/base/admin/license/import",
                "/api/base/admin/license/refresh",
                "/api/v1/license/validate",
                "/api/base/license/product/page",
                "/api/base/license/record/7/offline-file",
                "/health",
                "/actuator/health",
                "/outapi/anything",
                "/assets/app.js")) {
            assertTrue(interceptor.preHandle(request("GET", uri), new MockHttpServletResponse(), handler));
        }
        assertTrue(interceptor.preHandle(request("OPTIONS", "/api/base/admin/user/page"),
                new MockHttpServletResponse(), handler));
        verify(licenseManager, never()).getState();
    }

    @Test
    void handlesContextPath() throws Exception {
        when(licenseManager.getState()).thenReturn(LicenseState.ACTIVE);
        MockHttpServletRequest request = request("GET", "/admin/api/base/admin/user/page");
        request.setContextPath("/admin");

        assertDoesNotThrow(() -> interceptor.preHandle(request, new MockHttpServletResponse(), handler()));
        verify(licenseManager).getState();
    }

    private static MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(uri);
        return request;
    }

    private static HandlerMethod handler() throws NoSuchMethodException {
        TestController controller = new TestController();
        return new HandlerMethod(controller, TestController.class.getMethod("handle"));
    }

    private static class TestController {
        public void handle() {
        }
    }
}
