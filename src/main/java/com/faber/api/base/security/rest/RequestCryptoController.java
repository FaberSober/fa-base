package com.faber.api.base.security.rest;

import com.faber.api.base.security.biz.RequestCryptoBiz;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaNoLog;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.config.annotation.IgnoreLicense;
import com.faber.api.base.rbac.biz.RbacUserRoleBiz;
import com.faber.core.exception.auth.UserNoPermissionException;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@FaLogBiz("请求加密")
@FaNoLog
@ConditionalOnProperty(prefix = "fa.security.crypto", name = "enabled", havingValue = "true")
@RequestMapping("/api/base/security/crypto")
public class RequestCryptoController extends BaseResHandler {

    private final RequestCryptoBiz biz;
    private final RbacUserRoleBiz rbac;

    public RequestCryptoController(RequestCryptoBiz biz, RbacUserRoleBiz rbac) {
        this.biz = biz;
        this.rbac = rbac;
    }

    @GetMapping("/public-key")
    @IgnoreUserToken
    @IgnoreLicense
    public Ret<RequestCryptoBiz.PublicKeyInfo> publicKey() {
        return ok(biz.publicKey());
    }

    @PostMapping("/prepare")
    public Ret<RequestCryptoBiz.Preparation> prepare(@RequestBody PrepareRequest request) {
        var target = biz.target(request.path(), request.method());
        if (target.login() || target.permission() == null || !rbac.checkUserLinkUrl(BaseContextHandler.getUserId(), target.permission())) {
            throw new UserNoPermissionException("无权访问");
        }
        return ok(biz.prepare(request.path(), request.method(), BaseContextHandler.getUserId(), false));
    }

    @PostMapping("/prepare-login")
    @IgnoreUserToken
    @IgnoreLicense
    public Ret<RequestCryptoBiz.Preparation> prepareLogin(@RequestBody PrepareRequest request) {
        return ok(biz.prepare(request.path(), request.method(), null, true));
    }

    public record PrepareRequest(String path, String method) {}
}
