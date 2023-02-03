package com.faber.api.base.admin.rest;

import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.vo.query.UserAccountVo;
import com.faber.api.base.admin.vo.query.UserBatchUpdateDeptVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.config.annotation.ApiToken;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.vo.msg.Ret;
import com.faber.core.web.rest.BaseController;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@FaLogBiz("用户")
@RestController
@RequestMapping("/api/base/admin/user")
public class UserController extends BaseController<UserBiz, User, String> {

    @FaLogOpr("登录账户信息")
    @RequestMapping(value = "/getLoginUser", method = RequestMethod.GET)
    @ResponseBody
    public Ret<User> getLoginUser() {
        User o = baseBiz.getLoginUser();
        return ok(o);
    }

    @FaLogOpr("获取API账户信息")
    @RequestMapping(value = "/getApiUser", method = RequestMethod.GET)
    @ResponseBody
    @ApiToken
    public Ret<User> getApiUser() {
        User o = baseBiz.getLoginUser();
        return ok(o);
    }

    @FaLogOpr(value = "重置密码", crud = LogCrudEnum.C)
    @RequestMapping(value = "/resetPwd", method = RequestMethod.POST)
    @ResponseBody
    public Ret<Boolean> resetPwd(@RequestBody Map<String, Object> params) {
        baseBiz.resetPwd(params);
        return ok();
    }

    @FaLogOpr(value = "更新个人信息", crud = LogCrudEnum.C)
    @RequestMapping(value = "/updateMine", method = RequestMethod.POST)
    @ResponseBody
    public Ret<Boolean> updateMine(@Valid @RequestBody UserAccountVo vo) {
        baseBiz.updateMine(getCurrentUserId(), vo);
        return ok();
    }

    @FaLogOpr(value = "更新个人密码", crud = LogCrudEnum.C)
    @RequestMapping(value = "/updateMyPwd", method = RequestMethod.POST)
    @ResponseBody
    public Ret<Boolean> updateMyPwd(@RequestBody Map<String, Object> params) {
        baseBiz.updateMyPwd(getCurrentUserId(), params);
        return ok();
    }

    @FaLogOpr(value = "更新个人ApiToken", crud = LogCrudEnum.C)
    @RequestMapping(value = "/updateMyApiToken", method = RequestMethod.POST)
    @ResponseBody
    public Ret<Boolean> updateMyApiToken() {
        baseBiz.updateMyApiToken(getCurrentUserId());
        return ok();
    }

    @FaLogOpr(value = "批量更新部门", crud = LogCrudEnum.C)
    @RequestMapping(value = "/updateInfoBatch", method = RequestMethod.POST)
    @ResponseBody
    public Ret<Boolean> updateInfoBatch(@RequestBody UserBatchUpdateDeptVo params) {
        baseBiz.updateInfoBatch(params);
        return ok();
    }

    @FaLogOpr(value = "批量更新密码", crud = LogCrudEnum.C)
    @RequestMapping(value = "/accountAdminUpdatePwd", method = RequestMethod.POST)
    @ResponseBody
    public Ret<Boolean> accountAdminUpdatePwd(@RequestBody Map<String, Object> params) {
        baseBiz.accountAdminUpdatePwd(params);
        return ok();
    }

    @FaLogOpr(value = "批量删除账户", crud = LogCrudEnum.C)
    @RequestMapping(value = "/accountAdminDelete", method = RequestMethod.POST)
    @ResponseBody
    public Ret<Boolean> accountAdminDelete(@RequestBody Map<String, Object> params) {
        baseBiz.accountAdminDelete(params);
        return ok();
    }

}
