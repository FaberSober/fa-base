package com.faber.api.base.admin.biz;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.api.base.admin.entity.Notice;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.mapper.UserMapper;
import com.faber.api.base.msg.helper.MsgHelper;
import com.faber.api.base.msg.helper.config.MsgSendSysConfig;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.core.config.mybatis.handler.MysqlMetaObjectHandler;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.BuzzException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Mockito.*;

class NoticeBizTest {

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
    }

    @Test
    void tenantIdIsReadOnlyForAllJsonWriteEndpoints() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Notice notice = mapper.readValue("{\"id\":1,\"tenantId\":\"forged\",\"title\":\"公告\"}", Notice.class);
        assertNull(notice.getTenantId());
        notice.setTenantId("tenant-a");
        assertEquals("tenant-a", mapper.readTree(mapper.writeValueAsString(notice)).get("tenantId").asText());

    }

    @Test
    void insertsUseTrustedTenantAndRequireSelectedTenant() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Notice.class);
        Notice notice = notice();
        notice.setTenantId("forged");
        TenantContext.setTenantId("tenant-a");
        MysqlMetaObjectHandler handler = new MysqlMetaObjectHandler(setting(true));
        handler.insertFill(SystemMetaObject.forObject(notice));
        assertEquals("tenant-a", notice.getTenantId());
        TenantContext.clear();
        assertThrows(BuzzException.class, () -> handler.insertFill(SystemMetaObject.forObject(notice())));
    }

    @Test
    void sendsToTenantMembersWithIsolatedAsyncContext() {
        TenantUserBiz members = mock(TenantUserBiz.class);
        MsgHelper messages = mock(MsgHelper.class);
        when(members.getUserIdsByTenantId("tenant-a")).thenReturn(List.of("member-a"));
        AtomicReference<Runnable> task = new AtomicReference<>();
        NoticeBiz biz = biz(true, members, messages, task::set);
        BaseContextHandler.setUserId("author");
        TenantContext.setTenantId("tenant-a");
        doAnswer(invocation -> {
            assertEquals("tenant-a", TenantContext.getTenantId());
            return null;
        }).when(messages).sendSysMsg(eq("author"), aryEq(new String[]{"member-a"}), any(MsgSendSysConfig.class));
        biz.afterSave(notice());
        TenantContext.setTenantId("tenant-b");
        task.get().run();
        verify(messages).sendSysMsg(eq("author"), aryEq(new String[]{"member-a"}), any(MsgSendSysConfig.class));
        assertNull(BaseContextHandler.getHoldMap());
    }

    @Test
    void cleansAsyncContextOnFailure() {
        TenantUserBiz members = mock(TenantUserBiz.class);
        when(members.getUserIdsByTenantId("tenant-a")).thenThrow(new IllegalStateException("failed"));
        NoticeBiz biz = biz(true, members, mock(MsgHelper.class), Runnable::run);
        BaseContextHandler.setUserId("author");
        assertThrows(IllegalStateException.class, () -> biz.afterSave(notice()));
        assertNull(BaseContextHandler.getHoldMap());
    }

    @Test
    void disabledTenantModeStillNotifiesAllUsers() {
        TenantUserBiz members = mock(TenantUserBiz.class);
        MsgHelper messages = mock(MsgHelper.class);
        NoticeBiz biz = biz(false, members, messages, Runnable::run);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "test"), User.class);
        UserMapper mapper = mock(UserMapper.class);
        User user = new User();
        user.setId("user-a");
        when(mapper.selectList(any())).thenReturn(List.of(user));
        UserBiz users = new UserBiz() {
            @Override
            public Class<User> getEntityClass() {
                return User.class;
            }
        };
        ReflectionTestUtils.setField(users, "baseMapper", mapper);
        ReflectionTestUtils.setField(biz, "userBiz", users);
        BaseContextHandler.setUserId("author");
        biz.afterSave(notice());
        verify(messages).sendSysMsg(eq("author"), aryEq(new String[]{"user-a"}), any(MsgSendSysConfig.class));
        verifyNoInteractions(members);
    }

    private Notice notice() {
        Notice notice = new Notice();
        notice.setId(1);
        notice.setTenantId("tenant-a");
        notice.setTitle("公告");
        notice.setContent("内容");
        return notice;
    }

    private FaSetting setting(boolean enabled) {
        FaSetting setting = new FaSetting();
        FaSetting.Tenant tenant = new FaSetting.Tenant();
        tenant.setEnabled(enabled);
        setting.setTenant(tenant);
        return setting;
    }

    private NoticeBiz biz(boolean enabled, TenantUserBiz members, MsgHelper messages, Executor executor) {
        NoticeBiz biz = new NoticeBiz();
        ReflectionTestUtils.setField(biz, "faSetting", setting(enabled));
        ReflectionTestUtils.setField(biz, "tenantUserBiz", members);
        ReflectionTestUtils.setField(biz, "msgHelper", messages);
        ReflectionTestUtils.setField(biz, "executor", executor);
        return biz;
    }
}
