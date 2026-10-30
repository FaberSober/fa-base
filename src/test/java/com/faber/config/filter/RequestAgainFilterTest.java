package com.faber.config.filter;

import com.faber.api.base.admin.biz.ConfigSysBiz;
import com.faber.api.base.admin.biz.LogApiBiz;
import com.faber.core.constant.FaSetting;
import com.faber.core.vo.config.FaConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import com.faber.core.config.filter.wrapper.BodyHttpServletResponseWrapper;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class RequestAgainFilterTest {

    @Test
    void jobLogReadsAndDownloadsAreStreamedWithoutApiLogsEvenOnFailure() throws Exception {
        RequestAgainFilter filter = jobLogFilter();
        for (String[] route : new String[][]{{"GET", "/api/base/admin/jobLog/read/1"},
                {"POST", "/api/base/admin/jobLog/download/1"}}) {
            MockHttpServletRequest request = new MockHttpServletRequest(route[0], "/fa-admin" + route[1]);
            request.setContextPath("/fa-admin");
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, (req, res) -> {
                assertFalse(res instanceof BodyHttpServletResponseWrapper);
                BaseContextHandler.useAdmin();
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(404);
                res.getOutputStream().write("日志已清理".getBytes(StandardCharsets.UTF_8));
            });
            assertEquals(404, response.getStatus());
            assertEquals("1", response.getHeader("FaNoLog"));
            assertEquals("private, no-store", response.getHeader("Cache-Control"));
            assertEquals("日志已清理", response.getContentAsString(StandardCharsets.UTF_8));
            assertEquals(null, BaseContextHandler.getUserId());
        }
        verifyNoInteractions(filter.logApiBiz);
        assertFalse(RequestAgainFilter.isJobLogReadRequest("POST", "/api/base/admin/jobLog/read/1"));
        assertFalse(RequestAgainFilter.isJobLogReadRequest("GET", "/api/base/admin/jobLog/page"));
        assertFalse(RequestAgainFilter.isJobLogReadRequest("GET", "/api/base/admin/jobLog/read/1/extra"));
    }

    @Test
    void skippingJobLogPersistenceStillValidatesRequestSignature() {
        RequestAgainFilter filter = jobLogFilter();
        filter.faSetting.getSafety().setIntact(true);
        filter.faSetting.getSafety().setSecret("test-secret");
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/base/admin/jobLog/read/1");
        request.addHeader("timestamp", "100");
        // 使用框架实际的来源头，保证命中完整性检查。
        request.addHeader(com.faber.core.constant.CommonConstants.FA_FROM, "web");
        assertThrows(BuzzException.class, () -> filter.doFilter(request, new MockHttpServletResponse(),
                (req, res) -> { throw new AssertionError("签名失败不应调用业务接口"); }));
        verifyNoInteractions(filter.logApiBiz);
    }

    private RequestAgainFilter jobLogFilter() {
        RequestAgainFilter filter = new RequestAgainFilter();
        filter.configSysBiz = mock(ConfigSysBiz.class);
        when(filter.configSysBiz.getConfig()).thenReturn(new FaConfig());
        filter.logApiBiz = mock(LogApiBiz.class);
        filter.faSetting = new FaSetting();
        filter.faSetting.setSafety(new FaSetting.Safety());
        return filter;
    }

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
                "/api/base/admin/fileSave/getPreviewDownload/test-session", "/api/base/admin/fileSave/getPreviewThumbnail/test-session"}) {
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
