package com.faber.api.portal.file.rest;

import cn.dev33.satoken.stp.StpUtil;
import com.faber.api.base.admin.biz.FilePreviewTicketBiz;
import com.faber.api.base.admin.biz.FileSaveBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.biz.UserDeviceBiz;
import com.faber.api.base.admin.biz.UserTokenBiz;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.vo.ret.FilePreviewTicketRetVo;
import com.faber.config.auth.OnlineUserTracker;
import com.faber.config.auth.TenantContextResolver;
import com.faber.config.interceptor.UserAuthRestInterceptor;
import com.faber.config.interceptor.UserDeviceInterceptor;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.auth.UserDeviceInvalidException;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PortalFileSaveControllerTest {
    private MockMvc mvc;
    private FileSaveBiz files;
    private FilePreviewTicketBiz tickets;
    private UserDeviceBiz devices;
    private User user;
    private MockedStatic<StpUtil> login;

    @BeforeEach
    void setUp() {
        files = mock(FileSaveBiz.class);
        tickets = mock(FilePreviewTicketBiz.class);
        devices = mock(UserDeviceBiz.class);
        PortalFileSaveController controller = new PortalFileSaveController();
        ReflectionTestUtils.setField(controller, "fileSaveBiz", files);
        ReflectionTestUtils.setField(controller, "filePreviewTicketBiz", tickets);
        UserBiz users = mock(UserBiz.class);
        user = new User();
        user.setId("portal-user");
        user.setStatus(true);
        user.setAdminEnabled(false);
        when(users.getById(user.getId())).thenReturn(user);
        UserAuthRestInterceptor auth = new UserAuthRestInterceptor();
        ReflectionTestUtils.setField(auth, "userBiz", users);
        ReflectionTestUtils.setField(auth, "userTokenBiz", mock(UserTokenBiz.class));
        ReflectionTestUtils.setField(auth, "onlineUserTracker", mock(OnlineUserTracker.class));
        ReflectionTestUtils.setField(auth, "tenantContextResolver", mock(TenantContextResolver.class));
        UserDeviceInterceptor device = new UserDeviceInterceptor();
        ReflectionTestUtils.setField(device, "userDeviceBiz", devices);
        mvc = MockMvcBuilders.standaloneSetup(controller).addInterceptors(auth, device).build();
        login = mockStatic(StpUtil.class);
        login.when(StpUtil::getTokenValue).thenReturn("test-login-token");
        login.when(StpUtil::getLoginIdAsString).thenReturn(user.getId());
    }

    @AfterEach
    void tearDown() {
        login.close();
        BaseContextHandler.remove();
        TenantContext.clear();
    }

    @Test
    void mobileUserWithoutAdminQualificationCanUploadAndRequestTicket() throws Exception {
        FileSave file = new FileSave();
        file.setId("file-1");
        when(files.upload(any(org.springframework.web.multipart.MultipartFile.class))).thenReturn(file);
        when(tickets.createTicket("file-1")).thenReturn(new FilePreviewTicketRetVo("test-ticket", 123L));
        mvc.perform(upload()).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value("file-1"));
        mvc.perform(ticket()).andExpect(status().isOk()).andExpect(jsonPath("$.data.ticket").value("test-ticket"));
        verify(files).upload(any(org.springframework.web.multipart.MultipartFile.class));
        verify(tickets).createTicket("file-1");
        verify(devices, times(2)).validateMobileDeviceSession("MOBILE", "device-1");
    }

    @Test
    void anonymousAndExpiredSessionsCannotUploadOrIssueTickets() throws Exception {
        for (String token : new String[]{"", "expired"}) {
            login.when(StpUtil::getTokenValue).thenReturn(token);
            login.when(StpUtil::getLoginIdAsString).thenReturn(null);
            mvc.perform(upload()).andExpect(status().isUnauthorized());
            mvc.perform(ticket()).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(files, tickets, devices);
    }

    @Test
    void frozenUserCannotUploadOrIssueTickets() throws Exception {
        user.setStatus(false);
        mvc.perform(upload()).andExpect(status().isUnauthorized());
        mvc.perform(ticket()).andExpect(status().isUnauthorized());
        verifyNoInteractions(files, tickets, devices);
    }

    @Test
    void invalidMobileDeviceCannotReachEitherBusinessMethod() {
        doThrow(new UserDeviceInvalidException("设备已撤销")).when(devices)
                .validateMobileDeviceSession("MOBILE", "device-1");
        for (MockHttpServletRequestBuilder request : new MockHttpServletRequestBuilder[]{upload(), ticket()}) {
            ServletException error = assertThrows(ServletException.class, () -> mvc.perform(request));
            assertInstanceOf(UserDeviceInvalidException.class, error.getCause());
        }
        verifyNoInteractions(files, tickets);
    }

    @Test
    void missingFileAndBlankFileIdAreRejected() throws Exception {
        mvc.perform(multipart("/api/portal/fileSave/upload")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/portal/fileSave/createPreviewTicket")
                .contentType(MediaType.APPLICATION_JSON).content("{\"fileId\":\" \"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(files, tickets);
    }

    private MockHttpServletRequestBuilder upload() {
        return multipart("/api/portal/fileSave/upload")
                .file(new MockMultipartFile("file", "test.txt", "text/plain", new byte[]{1}))
                .header("FaFrom", "FaApp").header("FaClientInstanceId", "device-1");
    }

    private MockHttpServletRequestBuilder ticket() {
        return post("/api/portal/fileSave/createPreviewTicket")
                .header("FaFrom", "FaApp").header("FaClientInstanceId", "device-1")
                .contentType(MediaType.APPLICATION_JSON).content("{\"fileId\":\"file-1\"}");
    }
}
