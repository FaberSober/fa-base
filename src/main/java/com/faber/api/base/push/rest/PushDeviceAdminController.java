package com.faber.api.base.push.rest;

import com.faber.api.base.push.biz.PushDeviceAdminBiz;
import com.faber.api.base.push.vo.query.PushDeviceAdminQueryVo;
import com.faber.api.base.push.vo.ret.PushDeviceAdminVo;
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

@FaLogBiz("推送设备测试台")
@RestController
@RequestMapping("/api/base/admin/pushDevice")
public class PushDeviceAdminController extends BaseResHandler {
    @Resource
    private PushDeviceAdminBiz pushDeviceAdminBiz;

    @LogNoRet
    @FaLogOpr(value = "分页查询推送设备", crud = LogCrudEnum.R)
    @PostMapping("/page")
    public TableRet<PushDeviceAdminVo> page(@RequestBody BasePageQuery<PushDeviceAdminQueryVo> params) {
        return pushDeviceAdminBiz.page(params);
    }
}
