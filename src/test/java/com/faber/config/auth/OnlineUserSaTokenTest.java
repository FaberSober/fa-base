package com.faber.config.auth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpLogic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 使用真实 Sa-Token 1.38 验证共享会话及 web/portal 隔离，不连接外部 Redis。 */
class OnlineUserSaTokenTest {
    @Test
    void sharedWebTokenIsRevokedWhilePortalRemainsValid() {
        SaTokenDao previous = SaManager.getSaTokenDao();
        SaTokenDaoDefaultImpl dao = new SaTokenDaoDefaultImpl();
        try {
            SaManager.setSaTokenDao(dao);
            StpLogic logic = new StpLogic("online-user-test");
            logic.setConfig(new SaTokenConfig().setIsConcurrent(true).setIsShare(true).setTimeout(3600));
            String first = logic.createLoginSession("2", new SaLoginModel().setDevice("web"));
            String shared = logic.createLoginSession("2", new SaLoginModel().setDevice("web"));
            String portal = logic.createLoginSession("2", new SaLoginModel().setDevice("portal"));
            assertEquals(first, shared);
            assertNotEquals(first, portal);
            assertEquals("web", logic.getLoginDeviceByToken(first));
            assertEquals(1, logic.getTokenValueListByLoginId("2", "web").size());
            logic.kickoutByTokenValue(first);
            assertNull(logic.getLoginIdByToken(shared));
            assertEquals("2", logic.getLoginIdByToken(portal));
        } finally {
            SaManager.setSaTokenDao(previous);
            dao.destroy();
        }
    }
}
