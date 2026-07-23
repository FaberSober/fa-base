package com.faber.api.portal.auth.rest;

import cn.dev33.satoken.stp.SaTokenInfo;
import com.faber.api.base.admin.biz.AuthBiz;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.vo.query.UserRegistryVo;
import com.faber.api.portal.auth.vo.PortalLoginReqVo;
import com.faber.api.portal.auth.vo.PortalRegisterReqVo;
import com.faber.api.portal.auth.vo.PortalSessionRetVo;
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

@FaLogBiz("Portal-用户认证")
@RestController
@RequestMapping("/api/portal/auth")
public class PortalAuthController extends BaseResHandler {

    @Resource
    private AuthBiz authBiz;

    @Resource
    private UserBiz userBiz;

    @FaLogOpr(value = "Portal登录", crud = LogCrudEnum.C)
    @IgnoreUserToken
    @PostMapping("/login")
    public Ret<PortalSessionRetVo> login(@Valid @RequestBody PortalLoginReqVo reqVo) {
        LoginReqVo loginReq = new LoginReqVo(reqVo.getUsername(), reqVo.getPassword());
        SaTokenInfo tokenInfo = authBiz.portalLogin(loginReq);
        User user = userBiz.getLoginUser();
        return ok(PortalSessionRetVo.of(tokenInfo, PortalUserRetVo.from(user)));
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

        SaTokenInfo tokenInfo = authBiz.portalLogin(new LoginReqVo(reqVo.getUsername(), reqVo.getPassword()));
        User user = userBiz.getLoginUser();
        return ok(PortalSessionRetVo.of(tokenInfo, PortalUserRetVo.from(user)));
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
