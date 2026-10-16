package com.faber.config.auth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.spring.SaTokenContextForSpringInJakartaServlet;
import cn.dev33.satoken.stp.StpLogic;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

/** 验证项目现有 Sa-Token 登录 Cookie 可供无 Authorization 请求头的同源资源使用。 */
class FileResourceCookieTest {
    @Test
    void loginCookieRestoresResourceSessionAndLogoutRemovesIt() {
        SaTokenDao previousDao = SaManager.getSaTokenDao();
        SaTokenContext previousContext = SaManager.getSaTokenContext();
        SaTokenDaoDefaultImpl dao = new SaTokenDaoDefaultImpl();
        try {
            SaManager.setSaTokenDao(dao);
            SaManager.setSaTokenContext(new SaTokenContextForSpringInJakartaServlet());
            StpLogic logic = new StpLogic("file-cookie-test");
            logic.setConfig(new SaTokenConfig().setTokenName("Authorization").setTimeout(2592000));
            MockHttpServletRequest login = new MockHttpServletRequest("POST", "/api/base/admin/auth/login");
            MockHttpServletResponse loginResponse = new MockHttpServletResponse();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(login, loginResponse));
            logic.login("user-1", "web");
            String token = logic.getTokenValue();
            Cookie cookie = loginResponse.getCookie("Authorization");
            assertNotNull(cookie);
            assertEquals(token, cookie.getValue());
            assertEquals("/", cookie.getPath());
            assertEquals(2592000, cookie.getMaxAge());

            MockHttpServletRequest image = new MockHttpServletRequest("GET", "/api/base/admin/fileSave/getFilePreview/file-1");
            image.setCookies(cookie);
            MockHttpServletResponse resourceResponse = new MockHttpServletResponse();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(image, resourceResponse));
            assertNull(image.getHeader("Authorization"));
            assertEquals(token, logic.getTokenValue());
            assertEquals("user-1", logic.getLoginIdAsString());
            assertEquals("web", logic.getLoginDevice());

            logic.logout();
            assertNull(logic.getLoginIdByToken(token));
            Cookie removed = resourceResponse.getCookie("Authorization");
            assertNotNull(removed);
            assertEquals(0, removed.getMaxAge());
            assertEquals("/", removed.getPath());
        } finally {
            RequestContextHolder.resetRequestAttributes();
            SaManager.setSaTokenContext(previousContext);
            SaManager.setSaTokenDao(previousDao);
            dao.destroy();
        }
    }
}
