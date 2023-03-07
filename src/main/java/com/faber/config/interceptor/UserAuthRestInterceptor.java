package com.faber.config.interceptor;

import com.faber.api.base.admin.biz.UserBiz;
import com.faber.core.config.annotation.AdminOpr;
import com.faber.core.config.annotation.ApiToken;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.api.base.admin.entity.User;
import com.faber.config.utils.jwt.JWTInfo;
import com.faber.config.utils.user.UserCheckUtil;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.auth.UserTokenException;
import com.faber.core.utils.FaKeyUtils;
import com.faber.core.utils.FaRedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 用户授权过滤器
 * 1. 过滤request#header的jwt token授权字段：Authorization
 * @author xu.pengfei
 * @date 2022/11/28 11:33
 */
@Slf4j
public class UserAuthRestInterceptor extends AbstractInterceptor {

    @Autowired
    private UserBiz userBiz;

    @Autowired
    FaRedisUtils faRedisUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 配置该注解，说明在上下文中注入当前操作用户为admin
        AdminOpr adminOpr = getMethodAnno(handler, AdminOpr.class);
        if (adminOpr != null) {
            User user = userBiz.getById(1L);
            setUserLogin(user);
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
            setUserLogin(user);
            return super.preHandle(request, response, handler);
        }

        // type 2: 读取header中jwt用户信息
        String token = getToken(request);
        // 在redis中查询token
        String userId = faRedisUtils.getStr(FaKeyUtils.getTokenKey(token));
        if (userId == null) throw new UserTokenException("令牌失效，请重新登录！");

        // 判断用户状态是否正常
        User user = userBiz.getById(userId);
        setUserLogin(user);
        return super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        BaseContextHandler.remove(); // 放到Filter中去执行
        super.afterCompletion(request, response, handler, ex);
    }

    private void setUserLogin(User user) {
        UserCheckUtil.checkUserValid(user);

        BaseContextHandler.setUsername(user.getUsername());
        BaseContextHandler.setName(user.getName());
        BaseContextHandler.setUserId(user.getId());
        BaseContextHandler.setLogin(true);
    }
}
