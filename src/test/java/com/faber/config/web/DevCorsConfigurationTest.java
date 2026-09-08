package com.faber.config.web;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class DevCorsConfigurationTest {

    @Test
    void registersOnlyForDevProfile() {
        for (String profile : new String[]{"dev", "prod", "default"}) {
            try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
                context.getEnvironment().setActiveProfiles(profile);
                context.register(DevCorsConfiguration.class);
                context.refresh();
                assertEquals("dev".equals(profile), context.containsBean("corsFilterRegistration"));
            }
        }
    }

    @Test
    void acceptsPreflightForAnyOriginPathMethodAndHeaders() throws Exception {
        var registration = new DevCorsConfiguration().corsFilterRegistration();
        assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.getOrder());
        for (String path : new String[]{"/api/base/admin/user/page", "/outapi/example", "/portal/ai/agents/demo/chat", "/custom"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", path);
            request.addHeader("Origin", "http://arbitrary-client.local:4567");
            request.addHeader("Access-Control-Request-Method", "PATCH");
            request.addHeader("Access-Control-Request-Headers", "Authorization, timestamp, us, bs, X-Custom");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();
            registration.getFilter().doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
            assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
            assertEquals("PATCH", response.getHeader("Access-Control-Allow-Methods"));
            assertTrue(response.getHeader("Access-Control-Allow-Headers").contains("Authorization"));
            assertTrue(response.getHeader("Access-Control-Allow-Headers").contains("X-Custom"));
            assertEquals("3600", response.getHeader("Access-Control-Max-Age"));
            assertNull(response.getHeader("Access-Control-Allow-Credentials"));
            assertNull(chain.getRequest());
        }
    }

    @Test
    void actualRequestContinuesThroughFilterChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/base/admin/user/page");
        request.addHeader("Origin", "http://localhost:9000");
        request.addHeader("Authorization", "test-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        new DevCorsConfiguration().corsFilterRegistration().getFilter().doFilter(request, response, chain);
        assertSame(request, chain.getRequest());
        assertEquals("test-token", ((MockHttpServletRequest) chain.getRequest()).getHeader("Authorization"));
        assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
    }
}
