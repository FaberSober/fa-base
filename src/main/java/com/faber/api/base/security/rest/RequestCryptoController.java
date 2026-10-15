package com.faber.api.base.security.rest;

import com.faber.api.base.security.biz.RequestCryptoBiz;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaNoLog;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.config.annotation.IgnoreLicense;
import com.faber.core.config.annotation.Permission;
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

    public RequestCryptoController(RequestCryptoBiz biz) {
        this.biz = biz;
    }

    @GetMapping("/public-key")
    @IgnoreUserToken
    @IgnoreLicense
    public Ret<RequestCryptoBiz.PublicKeyInfo> publicKey() {
        return ok(biz.publicKey());
    }

    @PostMapping("/prepare")
    @Permission(permission = RequestCryptoBiz.DEMO_PERMISSION)
    public Ret<RequestCryptoBiz.Preparation> prepare(@RequestBody PrepareRequest request) {
        return ok(biz.prepare(request.purpose(), BaseContextHandler.getUserId()));
    }

    public record PrepareRequest(String purpose) {}
}
