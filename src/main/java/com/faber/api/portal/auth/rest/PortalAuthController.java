package com.faber.api.portal.auth.rest;

import cn.dev33.satoken.stp.SaTokenInfo;
import com.faber.api.base.admin.biz.AuthBiz;
import com.faber.api.base.admin.biz.SmsCodeBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.vo.query.UserRegistryVo;
import com.faber.api.portal.auth.vo.PortalLoginCodeReqVo;
import com.faber.api.portal.auth.vo.PortalLoginReqVo;
import com.faber.api.portal.auth.vo.PortalRegisterReqVo;
import com.faber.api.portal.auth.vo.PortalSessionRetVo;
import com.faber.api.portal.auth.vo.PortalSmsLoginReqVo;
import com.faber.api.portal.auth.vo.PortalUserRetVo;
import com.faber.config.utils.user.LoginReqVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

@FaLogBiz("Portal-用户认证")
@RestController
@Slf4j
@RequestMapping("/api/portal/auth")
public class PortalAuthController extends BaseResHandler {

    @Resource
    private AuthBiz authBiz;

    @Resource
    private UserBiz userBiz;

    @Resource
    private SmsCodeBiz smsCodeBiz;

    @FaLogOpr(value = "申请Portal登录验证码", crud = LogCrudEnum.C)
    @IgnoreUserToken
    @PostMapping("/send-login-code")
    public Ret<Void> sendLoginCode(@Valid @RequestBody PortalLoginCodeReqVo reqVo) {
        try {
            smsCodeBiz.requestLoginCode(reqVo.getPhone());
        } catch (RuntimeException e) {
            log.warn("Portal login code request failed");
        }
        return ok();
    }

    @FaLogOpr(value = "Portal登录", crud = LogCrudEnum.C)
    @IgnoreUserToken
    @PostMapping("/login")
    public Ret<PortalSessionRetVo> login(@Valid @RequestBody PortalLoginReqVo reqVo) {
        LoginReqVo loginReq = new LoginReqVo(reqVo.getUsername(), reqVo.getPassword());
        AuthBiz.PortalLoginResult loginResult = authBiz.portalLoginWithTrust(loginReq);
        User user = userBiz.getById(getCurrentUserId());
        return ok(PortalSessionRetVo.of(loginResult.session(), PortalUserRetVo.from(user),
                loginResult.deviceTrustToken()));
    }

    @FaLogOpr(value = "Portal短信登录", crud = LogCrudEnum.C)
    @IgnoreUserToken
    @PostMapping("/sms-login")
    public Ret<PortalSessionRetVo> loginBySms(@Valid @RequestBody PortalSmsLoginReqVo reqVo) {
        AuthBiz.PortalLoginResult loginResult = authBiz.portalLoginBySms(
                reqVo.getPhone(), reqVo.getVerificationCode());
        User user = userBiz.getById(getCurrentUserId());
        return ok(PortalSessionRetVo.of(loginResult.session(), PortalUserRetVo.from(user),
                loginResult.deviceTrustToken()));
    }

    @FaLogOpr(value = "Portal注册", crud = LogCrudEnum.C)
    @IgnoreUserToken
    @PostMapping("/register")
    public Ret<PortalSessionRetVo> register(@Valid @RequestBody PortalRegisterReqVo reqVo) {
        markPortalRegistrationActor(reqVo);

        UserRegistryVo registryVo = new UserRegistryVo();
        registryVo.setUsername(reqVo.getUsername());
        registryVo.setName(reqVo.getName());
        registryVo.setTel(reqVo.getTel());
        registryVo.setPassword(reqVo.getPassword());
        registryVo.setPasswordConfirm(reqVo.getPasswordConfirm());
        userBiz.registry(registryVo);

        AuthBiz.PortalLoginResult loginResult = authBiz.portalLoginWithTrust(
                new LoginReqVo(reqVo.getUsername(), reqVo.getPassword()));
        User user = userBiz.getById(getCurrentUserId());
        return ok(PortalSessionRetVo.of(loginResult.session(), PortalUserRetVo.from(user),
                loginResult.deviceTrustToken()));
    }

    @FaLogOpr(value = "Portal退出", crud = LogCrudEnum.C)
    @GetMapping("/logout")
    public Ret<Void> logout() {
        authBiz.logout();
        return ok();
    }

    private void markPortalRegistrationActor(PortalRegisterReqVo reqVo) {
        BaseContextHandler.setUserId("portal");
        BaseContextHandler.setUsername(reqVo.getUsername());
        BaseContextHandler.setName(reqVo.getName());
        BaseContextHandler.setLogin(true);
    }
}
