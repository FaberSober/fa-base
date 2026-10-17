package com.faber.config.interceptor;

import cn.dev33.satoken.stp.StpUtil;
import com.faber.core.annotation.FaFileAccess;
import com.faber.api.base.admin.biz.FileSaveBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.biz.UserDeviceBiz;
import com.faber.api.base.admin.biz.UserTokenBiz;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.rest.FileSaveController;
import com.faber.config.auth.OnlineUserTracker;
import com.faber.config.auth.TenantContextResolver;
import com.faber.core.constant.CommonConstants;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.BuzzException;
import com.faber.core.exception.auth.UserDeviceInvalidException;
import com.faber.core.exception.auth.UserNoPermissionException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PrivateFileAuthTest {

    private UserAuthRestInterceptor interceptor;
    private FileSaveBiz files;
    private UserBiz users;
    private TenantContextResolver tenants;
    private OnlineUserTracker online;

    @BeforeEach
    void setUp() {
        interceptor = new UserAuthRestInterceptor();
        files = mock(FileSaveBiz.class);
        users = mock(UserBiz.class);
        tenants = mock(TenantContextResolver.class);
        online = mock(OnlineUserTracker.class);
        ReflectionTestUtils.setField(interceptor, "fileSaveBiz", files);
        ReflectionTestUtils.setField(interceptor, "userBiz", users);
        ReflectionTestUtils.setField(interceptor, "userTokenBiz", mock(UserTokenBiz.class));
        ReflectionTestUtils.setField(interceptor, "tenantContextResolver", tenants);
        ReflectionTestUtils.setField(interceptor, "onlineUserTracker", online);
        FileSave file = new FileSave();
        file.setId("file-1");
        when(files.getById("file-1")).thenReturn(file);
        when(files.isPrivateQiniu(file)).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
        TenantContext.clear();
    }

    @Test
    void privateReadEndpointsRejectAnonymousRequestsDespiteIgnoreUserToken() throws Exception {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            for (String method : new String[]{"getFile", "getFilePreview", "getFileStr"}) {
                MockHttpServletRequest request = request(method);
                MockHttpServletResponse response = new MockHttpServletResponse();
                assertFalse(interceptor.preHandle(request, response, handler(method)));
                assertEquals(401, response.getStatus());
                assertEquals("private, no-store", response.getHeader("Cache-Control"));
                assertEquals(Boolean.TRUE, request.getAttribute(FaFileAccess.AUTH_REQUIRED));
            }
            stp.verify(StpUtil::getTokenValue, times(3));
        }
        verifyNoInteractions(users);
    }

    @Test
    void publicFilesKeepAnonymousAccess() throws Exception {
        when(files.isPrivateQiniu(any())).thenReturn(false);
        for (String method : new String[]{"getFile", "getFilePreview", "getFileStr"}) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertTrue(interceptor.preHandle(request(method), response, handler(method)));
            assertNull(response.getHeader("Cache-Control"));
        }
        verifyNoInteractions(users);
    }

    @Test
    void configuredPublicResourcesAllowOnlyOriginalAndThumbnailAccess() throws Exception {
        when(files.isPublicSystemResource("file-1")).thenReturn(true);
        assertTrue(interceptor.preHandle(request("getFile"), new MockHttpServletResponse(), handler("getFile")));
        assertTrue(interceptor.preHandle(request("getFilePreview"), new MockHttpServletResponse(), handler("getFilePreview")));
        try (MockedStatic<StpUtil> ignored = mockStatic(StpUtil.class)) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertFalse(interceptor.preHandle(request("getFileStr"), response, handler("getFileStr")));
            assertEquals(401, response.getStatus());
        }
    }

    @Test
    void validLoginReusesUserTenantAndDeviceChecks() throws Exception {
        User user = activeUser();
        when(users.getById("user-1")).thenReturn(user);
        MockHttpServletRequest request = request("getFile");
        MockHttpServletResponse response = new MockHttpServletResponse();
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("test-token");
            stp.when(StpUtil::getLoginIdAsString).thenReturn("user-1");
            assertTrue(interceptor.preHandle(request, response, handler("getFile")));
            verify(users).setUserLogin(user);
            verify(tenants).resolve(request, "user-1");
            verify(online).touch("test-token", user, false, null, null);
        }
        request.addHeader(CommonConstants.FA_FROM, CommonConstants.FaFrom.FaApp);
        request.addHeader(CommonConstants.FA_APP_DEVICE_ID, "invalid-device");
        UserDeviceBiz devices = mock(UserDeviceBiz.class);
        UserDeviceInterceptor deviceInterceptor = new UserDeviceInterceptor();
        ReflectionTestUtils.setField(deviceInterceptor, "userDeviceBiz", devices);
        assertThrows(UserDeviceInvalidException.class,
                () -> deviceInterceptor.preHandle(request, response, handler("getFile")));
        verify(devices).getByDeviceId("invalid-device");
    }

    @Test
    void frozenAccountAndDisabledAdminAccessCannotReadPrivateFiles() throws Exception {
        User user = activeUser();
        user.setStatus(false);
        when(users.getById("user-1")).thenReturn(user);
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("test-token");
            stp.when(StpUtil::getLoginIdAsString).thenReturn("user-1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertFalse(interceptor.preHandle(request("getFile"), response, handler("getFile")));
            assertEquals(401, response.getStatus());
            user.setStatus(true);
            user.setAdminEnabled(false);
            assertThrows(UserNoPermissionException.class,
                    () -> interceptor.preHandle(request("getFile"), new MockHttpServletResponse(), handler("getFile")));
        }
    }

    @Test
    void missingFileFailsBeforeAnyDownload() {
        when(files.getById("file-1")).thenReturn(null);
        assertThrows(BuzzException.class,
                () -> interceptor.preHandle(request("getFile"), new MockHttpServletResponse(), handler("getFile")));
    }

    @Test
    void expiredLoginCannotReadPrivateFile() throws Exception {
        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("expired-token");
            stp.when(StpUtil::getLoginIdAsString).thenThrow(new IllegalStateException("expired"));
            MockHttpServletResponse response = new MockHttpServletResponse();
            assertFalse(interceptor.preHandle(request("getFile"), response, handler("getFile")));
            assertEquals(401, response.getStatus());
            verifyNoInteractions(users);
        }
    }

    private User activeUser() {
        User user = new User();
        user.setId("user-1");
        user.setStatus(true);
        user.setAdminEnabled(true);
        return user;
    }

    private HandlerMethod handler(String method) throws NoSuchMethodException {
        return new HandlerMethod(new FileSaveController(), method, String.class);
    }

    private MockHttpServletRequest request(String method) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/base/admin/fileSave/" + method + "/file-1");
        request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of("fileId", "file-1"));
        return request;
    }
}
