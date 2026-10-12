package com.faber.config.filter;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestAgainFilterTest {

    @Test
    void shouldMatchSkipUrlContainingPathVariable() {
        RequestAgainFilter.addSkipUrl("/api/ai/agent/public/{accessToken}/chat");

        assertTrue(RequestAgainFilter.matchesSkipUrl(
                "/api/ai/agent/public/3db533999c5d411097d4c540052acefe/chat"));
        assertFalse(RequestAgainFilter.matchesSkipUrl(
                "/api/ai/agent/public/3db533999c5d411097d4c540052acefe/conversation"));
    }

    @Test
    void shouldRemoveContextPathBeforeMatching() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/fa-admin");
        request.setRequestURI("/fa-admin/api/ai/agent/openapi/chat");

        assertEquals("/api/ai/agent/openapi/chat", RequestAgainFilter.getRequestPath(request));
    }
}
