package com.faber.api.base.admin.rest;

import com.faber.api.base.admin.biz.RemoteClientBiz;
import com.faber.api.base.admin.vo.query.RemoteClientQueryVo;
import com.faber.api.base.admin.vo.ret.RemoteClientVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FaLogBiz("在线客户端")
@RestController
@RequestMapping("/api/base/admin/remoteClient")
public class RemoteClientController extends BaseResHandler {
    @Resource
    private RemoteClientBiz remoteClientBiz;

    @LogNoRet
    @FaLogOpr(value = "查询在线客户端", crud = LogCrudEnum.R)
    @PostMapping("/page")
    public TableRet<RemoteClientVo> page(@RequestBody BasePageQuery<RemoteClientQueryVo> params) {
        return remoteClientBiz.page(params);
    }
}
