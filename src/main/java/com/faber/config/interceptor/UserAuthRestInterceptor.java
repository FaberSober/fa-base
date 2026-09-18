package com.faber.config.interceptor;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.biz.UserTokenBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.entity.UserToken;
import com.faber.config.auth.OnlineUserTracker;
import com.faber.config.auth.TenantContextResolver;
import com.faber.core.config.annotation.AdminOpr;
import com.faber.core.config.annotation.ApiToken;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.auth.UserTokenException;
import com.faber.core.exception.auth.UserNoPermissionException;
import com.faber.core.vo.msg.BaseRet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 用户授权过滤器
 * 1. 过滤request#header的jwt token授权字段：Authorization
 * @author farando
 * @date 2022/11/28 11:33
 */
@Slf4j
@Component
public class UserAuthRestInterceptor extends AbstractInterceptor {

    @Resource
    private UserBiz userBiz;

    @Resource
    private UserTokenBiz userTokenBiz;

    @Resource
    private OnlineUserTracker onlineUserTracker;

    @Resource
    private TenantContextResolver tenantContextResolver;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            return doPreHandle(request, response, handler);
        } catch (UserTokenException e) {
            log.warn("用户认证失败：{}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(JSONUtil.toJsonStr(new BaseRet(e.getStatus(), e.getMessage())));
            return false;
        }
    }

    private boolean doPreHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        TenantContext.clear();

        // 配置该注解，说明在上下文中注入当前操作用户为admin
        AdminOpr adminOpr = getMethodAnno(handler, AdminOpr.class);
        if (adminOpr != null) {
            User user = userBiz.getById(1L);
            userBiz.setUserLogin(user);
            return super.preHandle(request, response, handler);
        }

        // 配置该注解，说明不进行用户拦截
        IgnoreUserToken ignoreUserToken = getMethodAnno(handler, IgnoreUserToken.class);
        if (ignoreUserToken != null) {
            return super.preHandle(request, response, handler);
        }

        // type 1: 读取API信息
        ApiToken apiToken = getMethodAnno(handler, ApiToken.class);
        if (apiToken != null) {
            User user = userBiz.getUserFromApiToken();
            userBiz.setUserLogin(user, "api");
            tenantContextResolver.resolve(request, user.getId());
            return super.preHandle(request, response, handler);
        }

        // type 2: 读取header中jwt用户信息
        String token = StpUtil.getTokenValue();
        if (StrUtil.isEmpty(token)) {
            throw new UserTokenException("令牌失效，请重新登录！");
        }

        // sa-token获取登录账户信息
        String userId = null;
        try {
            userId = StpUtil.getLoginIdAsString();
        } catch (Exception e) {
            if (e instanceof NotLoginException notLogin && NotLoginException.KICK_OUT.equals(notLogin.getType())) {
                throw new UserTokenException("您已被管理员强制下线，请重新登录！");
            }
            // 这里先尝试使用API Token，最终是否认证失败由后续的userId判断统一记录。
            if (!(e instanceof UserTokenException || e instanceof NotLoginException)) {
                log.warn(e.getMessage());
            }

            // 尝试ApiToken登录
            UserToken userToken = userTokenBiz.getById(token);
            if (userToken != null && userToken.getValid()) {
                userId = userToken.getUserId();
            }
        }

        if (userId == null) {
            throw new UserTokenException("令牌失效，请重新登录！");
        }

        // 用户登录状态设置
        User user = userBiz.getById(userId);
        // token 解析成功但账户已不存在或被冻结，本质仍是登录态失效，统一抛 UserTokenException(401) 触发前端跳转登录页
        if (user == null || !Boolean.TRUE.equals(user.getStatus())) {
            throw new UserTokenException("账户不存在或被冻结，请重新登录");
        }
        userBiz.setUserLogin(user);
        requireApplicationAccess(request.getRequestURI(), user);
        tenantContextResolver.resolve(request, userId);

        onlineUserTracker.touch(token, user, false);

        return super.preHandle(request, response, handler);
    }

    static void requireApplicationAccess(String requestUri, User user) {
        if (requiresAdminAccess(requestUri) && !Boolean.TRUE.equals(user.getAdminEnabled())) {
            throw new UserNoPermissionException("当前账号未开通后台访问权限");
        }
    }

    static boolean requiresAdminAccess(String requestUri) {
        return !(requestUri.equals("/api/portal") || requestUri.startsWith("/api/portal/"));
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        BaseContextHandler.remove(); // 放到Filter中去执行
        super.afterCompletion(request, response, handler, ex);
    }

}
