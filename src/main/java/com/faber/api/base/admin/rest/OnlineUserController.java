package com.faber.api.base.admin.rest;

import com.faber.api.base.admin.biz.OnlineUserBiz;
import com.faber.api.base.admin.vo.query.OnlineUserKickoutVo;
import com.faber.api.base.admin.vo.query.OnlineUserQueryVo;
import com.faber.api.base.admin.vo.ret.OnlineUserStatsVo;
import com.faber.api.base.admin.vo.ret.OnlineUserVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@FaLogBiz("在线用户")
@RestController
@RequestMapping("/api/base/admin/onlineUser")
public class OnlineUserController extends BaseResHandler {
    @Resource
    private OnlineUserBiz onlineUserBiz;

    @LogNoRet
    @FaLogOpr(value = "查询后台在线会话", crud = LogCrudEnum.R)
    @PostMapping("/page")
    public TableRet<OnlineUserVo> page(@RequestBody BasePageQuery<OnlineUserQueryVo> params) {
        return onlineUserBiz.page(params);
    }

    @LogNoRet
    @FaLogOpr(value = "在线会话统计", crud = LogCrudEnum.R)
    @GetMapping("/stats")
    public Ret<OnlineUserStatsVo> stats() {
        return ok(onlineUserBiz.stats());
    }

    @FaLogOpr(value = "强制下线后台会话", crud = LogCrudEnum.U)
    @PostMapping("/kickout")
    public Ret<Integer> kickout(@Valid @RequestBody OnlineUserKickoutVo params) {
        return ok(onlineUserBiz.kickout(params));
    }
}
