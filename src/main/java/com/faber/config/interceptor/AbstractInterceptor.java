package com.faber.config.interceptor;

import com.faber.config.utils.jwt.JWTInfo;
import com.faber.config.utils.user.JwtTokenUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.lang.annotation.Annotation;

public abstract class AbstractInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    protected <A extends Annotation> A getMethodAnno(Object handler, Class<A> annoClazz) {
        if (!(handler instanceof HandlerMethod)) {
            return null;
        }
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        A annotation = handlerMethod.getBeanType().getAnnotation(annoClazz);
        if (annotation == null) {
            annotation = handlerMethod.getMethodAnnotation(annoClazz);
        }
        return annotation;
    }

    protected String getToken(HttpServletRequest request) {
        return request.getHeader(jwtTokenUtil.getTokenHeader());
    }

    protected JWTInfo getJwtInfo(HttpServletRequest request) {
        String token = request.getHeader(jwtTokenUtil.getTokenHeader());
        if (StringUtils.isEmpty(token)) {
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (cookie.getName().equals(jwtTokenUtil.getTokenHeader())) {
                        token = cookie.getValue();
                    }
                }
            }
        }
        return jwtTokenUtil.parseToken(token);
    }

}
