package com.faber.api.portal.auth.rest;

import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.portal.auth.vo.PortalUserRetVo;
import com.faber.config.utils.user.UserCheckUtil;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FaLogBiz("Portal-用户中心")
@RestController
@RequestMapping("/api/portal/account")
public class PortalAccountController extends BaseResHandler {

    @Resource
    private UserBiz userBiz;

    @GetMapping("/me")
    public Ret<PortalUserRetVo> me() {
        User user = userBiz.getById(getCurrentUserId());
        UserCheckUtil.checkUserValid(user);
        return ok(PortalUserRetVo.from(user));
    }
}
