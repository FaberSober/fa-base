package com.faber.api.portal.push.rest;

import com.faber.api.base.push.biz.PushDeviceBiz;
import com.faber.api.portal.push.vo.PortalPushDeviceBindingReqVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.config.annotation.IgnoreUserDevice;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FaLogBiz("Portal-推送设备")
@RestController
@RequestMapping("/api/portal/push/device")
public class PortalPushDeviceController extends BaseResHandler {

    @Resource
    private PushDeviceBiz pushDeviceBiz;

    @FaLogOpr(value = "登记推送设备", crud = LogCrudEnum.C)
    @LogNoRet
    @IgnoreUserDevice
    @PostMapping("/register")
    public Ret<Void> register(@Valid @RequestBody PortalPushDeviceBindingReqVo reqVo) {
        pushDeviceBiz.registerCurrentUser(reqVo);
        return ok();
    }

    @FaLogOpr(value = "注销推送设备", crud = LogCrudEnum.U)
    @LogNoRet
    @IgnoreUserDevice
    @PostMapping("/unregister")
    public Ret<Void> unregister(@Valid @RequestBody PortalPushDeviceBindingReqVo reqVo) {
        pushDeviceBiz.unregisterCurrentUser(reqVo);
        return ok();
    }
}
