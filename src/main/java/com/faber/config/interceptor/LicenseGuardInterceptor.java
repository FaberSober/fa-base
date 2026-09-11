package com.faber.config.interceptor;

import com.faber.core.exception.license.LicenseInvalidException;
import com.faber.core.license.LicenseManager;
import com.faber.core.license.LicenseState;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Set;

/**
 * Blocks business APIs when the current license cannot authorize the request.
 */
@Component
public class LicenseGuardInterceptor extends AbstractInterceptor {

    private static final Set<String> WHITELIST = Set.of(
            "/api/base/admin/auth/login",
            "/api/base/admin/auth/loginByToken",
            "/api/portal/auth/login",
            "/api/portal/auth/register",
            "/api/base/admin/license/info",
            "/api/base/admin/license/import",
            "/api/base/admin/license/refresh",
            "/api/v1/license/validate"
    );

    private final LicenseManager licenseManager;

    public LicenseGuardInterceptor(LicenseManager licenseManager) {
        this.licenseManager = licenseManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String uri = normalizeUri(request.getRequestURI(), request.getContextPath());
        if (!isApi(uri)
                || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || WHITELIST.contains(uri)
                || uri.startsWith("/api/base/license/")) {
            return true;
        }

        LicenseState state = licenseManager.getState();
        if (state == LicenseState.ACTIVE
                || state == LicenseState.GRACE
                || state == LicenseState.BYPASSED) {
            return true;
        }

        throw new LicenseInvalidException();
    }

    static String normalizeUri(String requestUri, String contextPath) {
        if (requestUri == null || contextPath == null || contextPath.isEmpty()) {
            return requestUri;
        }
        if (requestUri.equals(contextPath)) {
            return "/";
        }
        String contextPrefix = contextPath.endsWith("/") ? contextPath : contextPath + "/";
        return requestUri.startsWith(contextPrefix)
                ? requestUri.substring(contextPath.length())
                : requestUri;
    }

    static boolean isApi(String uri) {
        return uri != null && ("/api".equals(uri) || uri.startsWith("/api/"));
    }
}
