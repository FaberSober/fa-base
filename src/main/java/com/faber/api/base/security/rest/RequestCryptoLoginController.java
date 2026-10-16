package com.faber.api.base.security.rest;

import cn.dev33.satoken.stp.SaTokenInfo;
import com.faber.api.base.admin.biz.AuthBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.security.biz.RequestCryptoBiz;
import com.faber.api.portal.auth.vo.PortalSessionRetVo;
import com.faber.api.portal.auth.vo.PortalUserRetVo;
import com.faber.config.utils.user.LoginReqVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaNoLog;
import com.faber.core.config.annotation.IgnoreLicense;
import com.faber.core.config.annotation.IgnoreUserToken;
import jakarta.servlet.http.HttpServletRequest;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FaLogBiz("鉴权")
@FaNoLog
@ConditionalOnProperty(prefix = "fa.security.crypto", name = "enabled", havingValue = "true")
public class RequestCryptoLoginController extends BaseResHandler {

    private final RequestCryptoBiz crypto;
    private final AuthBiz auth;
    private final UserBiz users;

    public RequestCryptoLoginController(RequestCryptoBiz crypto, AuthBiz auth, UserBiz users) {
        this.crypto = crypto;
        this.auth = auth;
        this.users = users;
    }

    @PostMapping(RequestCryptoBiz.ADMIN_LOGIN_PATH)
    @IgnoreUserToken
    @IgnoreLicense
    public Ret<SaTokenInfo> adminLogin(@RequestBody LoginReqVo body, HttpServletRequest request) {
        body.setPassword(crypto.decryptField(body.getPassword(), request, "password"));
        return ok(auth.login(body));
    }

    @PostMapping(RequestCryptoBiz.PORTAL_LOGIN_PATH)
    @IgnoreUserToken
    @IgnoreLicense
    public Ret<PortalSessionRetVo> portalLogin(@RequestBody LoginReqVo body, HttpServletRequest request) {
        body.setPassword(crypto.decryptField(body.getPassword(), request, "password"));
        var result = auth.portalLoginWithTrust(body);
        var user = users.getById(getCurrentUserId());
        return ok(PortalSessionRetVo.of(result.session(), PortalUserRetVo.from(user), result.deviceTrustToken()));
    }
}
