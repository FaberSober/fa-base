package com.faber.config.filter;

import com.faber.api.base.admin.biz.ConfigSysBiz;
import com.faber.api.base.admin.biz.LogApiBiz;
import com.faber.core.constant.FaSetting;
import com.faber.core.vo.config.FaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

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

    @Test
    void previewCredentialRequestsAreNotCachedOrLoggedEvenOnFailure() throws Exception {
        RequestAgainFilter filter = new RequestAgainFilter();
        filter.configSysBiz = mock(ConfigSysBiz.class);
        when(filter.configSysBiz.getConfig()).thenReturn(new FaConfig());
        filter.logApiBiz = mock(LogApiBiz.class);
        filter.faSetting = new FaSetting();
        filter.faSetting.setSafety(new FaSetting.Safety());
        for (String path : new String[]{"/api/portal/fileSave/createPreviewTicket", "/api/base/admin/fileSave/createPreviewTicket",
                "/api/base/admin/fileSave/exchangePreviewTicket", "/api/base/admin/fileSave/getPreviewFile/test-session",
                "/api/base/admin/fileSave/getPreviewDownload/test-session"}) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
            request.setContent("{\"ticket\":\"test-secret\"}".getBytes(StandardCharsets.UTF_8));
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(401);
                res.getWriter().write("{\"message\":\"凭证无效\"}");
            });
            assertEquals(401, response.getStatus());
            assertEquals("private, no-store", response.getHeader("Cache-Control"));
            assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
        }
        verifyNoInteractions(filter.logApiBiz);
        assertFalse(RequestAgainFilter.isFilePreviewCredentialRequest("/api/base/admin/fileSave/getFile/file-1"));
        assertFalse(RequestAgainFilter.isFilePreviewCredentialRequest("/api/base/admin/fileSave/exchangePreviewTicketExtra"));
        assertFalse(RequestAgainFilter.isFilePreviewCredentialRequest("/api/portal/fileSave/createPreviewTicketExtra"));
    }
}
