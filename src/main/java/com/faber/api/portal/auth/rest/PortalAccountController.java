package com.faber.api.portal.auth.rest;

import com.faber.api.base.admin.biz.UserDeviceBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.portal.auth.vo.PortalLoginDeviceRetVo;
import com.faber.api.portal.auth.vo.PortalUserRetVo;
import com.faber.config.utils.user.UserCheckUtil;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@FaLogBiz("Portal-用户中心")
@RestController
@RequestMapping("/api/portal/account")
public class PortalAccountController extends BaseResHandler {

    @Resource
    private UserBiz userBiz;

    @Resource
    private UserDeviceBiz userDeviceBiz;

    @GetMapping("/me")
    public Ret<PortalUserRetVo> me() {
        User user = userBiz.getById(getCurrentUserId());
        UserCheckUtil.checkUserValid(user);
        return ok(PortalUserRetVo.from(user));
    }

    @FaLogOpr(value = "查询本人登录设备", crud = LogCrudEnum.R)
    @LogNoRet
    @GetMapping("/devices")
    public Ret<List<PortalLoginDeviceRetVo>> devices(
            @RequestHeader(value = "FaDeviceModel", required = false) String deviceModel,
            @RequestHeader(value = "FaDeviceBrand", required = false) String deviceBrand,
            @RequestHeader(value = "FaOsName", required = false) String osName,
            @RequestHeader(value = "FaOsVersion", required = false) String osVersion
    ) {
        return ok(userDeviceBiz.listPortalLoginDevices(
                deviceModel, deviceBrand, osName, osVersion));
    }

    @FaLogOpr(value = "设为不信任", crud = LogCrudEnum.U)
    @LogNoRet
    @PostMapping("/devices/{deviceId}/revoke-trust")
    public Ret<Void> revokeDeviceTrust(@PathVariable Integer deviceId) {
        userDeviceBiz.revokePortalDeviceTrust(deviceId);
        return ok();
    }
}
