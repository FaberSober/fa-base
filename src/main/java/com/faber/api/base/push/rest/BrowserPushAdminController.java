package com.faber.api.base.push.rest;

import com.faber.api.base.push.biz.BrowserPushSubscriptionBiz;
import com.faber.api.base.push.vo.req.BrowserPushSubscriptionEndpointReqVo;
import com.faber.api.base.push.vo.req.BrowserPushSubscriptionReqVo;
import com.faber.api.base.push.vo.req.BrowserPushTestSendReqVo;
import com.faber.api.base.push.vo.ret.BrowserPushSubscriptionStatusRetVo;
import com.faber.api.base.push.vo.ret.BrowserPushVapidPublicKeyRetVo;
import com.faber.api.base.push.vo.ret.BrowserPushTestSendRetVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FaLogBiz("浏览器推送订阅")
@RestController
@RequestMapping("/api/base/admin/webPush")
public class BrowserPushAdminController extends BaseResHandler {

    @Resource
    private BrowserPushSubscriptionBiz browserPushSubscriptionBiz;

    @PostMapping("/vapidPublicKey")
    public Ret<BrowserPushVapidPublicKeyRetVo> vapidPublicKey() {
        return ok(new BrowserPushVapidPublicKeyRetVo(browserPushSubscriptionBiz.getVapidPublicKey()));
    }

    @PostMapping("/subscription/status")
    public Ret<BrowserPushSubscriptionStatusRetVo> status(@Valid @RequestBody BrowserPushSubscriptionEndpointReqVo reqVo) {
        return ok(browserPushSubscriptionBiz.status(reqVo.getEndpoint()));
    }

    @PostMapping("/subscription/register")
    public Ret<Void> register(@Valid @RequestBody BrowserPushSubscriptionReqVo reqVo) {
        browserPushSubscriptionBiz.registerCurrentUser(reqVo);
        return ok();
    }

    @PostMapping("/subscription/unregister")
    public Ret<Void> unregister(@Valid @RequestBody BrowserPushSubscriptionEndpointReqVo reqVo) {
        browserPushSubscriptionBiz.unregisterCurrentUser(reqVo.getEndpoint());
        return ok();
    }

    @PostMapping("/test/send")
    public Ret<BrowserPushTestSendRetVo> sendTest(@Valid @RequestBody BrowserPushTestSendReqVo reqVo) {
        return ok(browserPushSubscriptionBiz.sendTestToCurrentUser(reqVo));
    }
}
