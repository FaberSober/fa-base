package com.faber.config.web;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DesktopCorsConfigurationTest {

    @Test
    void desktopPreflightAllowsLoginAndTelemetryHeadersBeforeAuthentication() throws Exception {
        for (String origin : new String[]{"http://tauri.localhost", "https://tauri.localhost", "tauri://localhost"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/base/admin/auth/login");
            request.addHeader("Origin", origin);
            request.addHeader("Access-Control-Request-Method", "POST");
            request.addHeader("Access-Control-Request-Headers", "content-type,authorization,fafrom,faversioncode,faversionname");
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicBoolean reachedAuthentication = new AtomicBoolean();

            new DesktopCorsConfiguration().desktopCorsFilterRegistration().getFilter()
                    .doFilter(request, response, (req, res) -> reachedAuthentication.set(true));

            assertEquals(200, response.getStatus());
            assertEquals(origin, response.getHeader("Access-Control-Allow-Origin"));
            assertTrue(response.getHeader("Access-Control-Allow-Headers").contains("authorization"));
            assertFalse(reachedAuthentication.get());
        }
    }

    @Test
    void actualDesktopRequestStillReachesAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/base/admin/auth/login");
        request.addHeader("Origin", "http://tauri.localhost");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean reachedAuthentication = new AtomicBoolean();

        new DesktopCorsConfiguration().desktopCorsFilterRegistration().getFilter()
                .doFilter(request, response, (req, res) -> reachedAuthentication.set(true));

        assertTrue(reachedAuthentication.get());
        assertEquals("http://tauri.localhost", response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void otherOriginsAndRequestsWithoutOriginKeepExistingPolicy() throws Exception {
        for (String origin : new String[]{null, "https://other.example", "http://tauri.localhost.attacker.example"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/base/admin/auth/login");
            if (origin != null) {
                request.addHeader("Origin", origin);
            }
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicBoolean reachedExistingPolicy = new AtomicBoolean();

            new DesktopCorsConfiguration().desktopCorsFilterRegistration().getFilter()
                    .doFilter(request, response, (req, res) -> reachedExistingPolicy.set(true));

            assertTrue(reachedExistingPolicy.get());
            assertNull(response.getHeader("Access-Control-Allow-Origin"));
        }
    }
}
