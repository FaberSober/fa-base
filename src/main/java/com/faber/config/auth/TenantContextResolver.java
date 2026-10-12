package com.faber.config.auth;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.core.constant.CommonConstants;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.TenantContext;
import com.faber.core.exception.auth.UserTokenException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

/**
 * 登录请求的租户上下文解析。
 */
@Component
public class TenantContextResolver {

    @Resource
    private TenantUserBiz tenantUserBiz;

    @Resource
    private FaSetting faSetting;

    public void resolve(HttpServletRequest request, String userId) {
        TenantContext.clear();
        if (!faSetting.isTenantEnabled()) {
            return;
        }

        String tenantId = request.getHeader(CommonConstants.FA_TN_TENANT_ID);
        if (StrUtil.isNotBlank(tenantId)) {
            if (!tenantUserBiz.hasUserTenant(userId, tenantId)) {
                throw new UserTokenException("当前账户无权访问该租户");
            }
        } else {
            tenantId = tenantUserBiz.getDefaultTenantId(userId);
        }

        if (StrUtil.isBlank(tenantId) && !TenantContext.isSuperAdmin(userId)) {
            throw new UserTokenException("当前账户未绑定可用租户");
        }
        TenantContext.setTenantId(tenantId);
    }
}
