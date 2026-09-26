package com.faber.api.base.push.rest;

import com.faber.api.base.push.biz.PushDeviceAdminBiz;
import com.faber.api.base.push.biz.PushTestAdminBiz;
import com.faber.api.base.push.vo.query.PushDeviceAdminQueryVo;
import com.faber.api.base.push.vo.req.PushTestSendReqVo;
import com.faber.api.base.push.vo.req.PushTestStatusReqVo;
import com.faber.api.base.push.vo.ret.PushDeviceAdminVo;
import com.faber.api.base.push.vo.ret.PushTestRunAdminVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@FaLogBiz("推送设备测试台")
@RestController
@RequestMapping("/api/base/admin/pushDevice")
public class PushDeviceAdminController extends BaseResHandler {
    @Resource
    private PushDeviceAdminBiz pushDeviceAdminBiz;
    @Resource
    private PushTestAdminBiz pushTestAdminBiz;

    @LogNoRet
    @FaLogOpr(value = "分页查询推送设备", crud = LogCrudEnum.R)
    @PostMapping("/page")
    public TableRet<PushDeviceAdminVo> page(@RequestBody BasePageQuery<PushDeviceAdminQueryVo> params) {
        return pushDeviceAdminBiz.page(params);
    }

    @LogNoRet
    @FaLogOpr(value = "发送定向测试推送", crud = LogCrudEnum.C)
    @PostMapping("/test/send")
    public PushTestRunAdminVo sendTest(@Valid @RequestBody PushTestSendReqVo reqVo) {
        return pushTestAdminBiz.send(reqVo);
    }

    @LogNoRet
    @FaLogOpr(value = "查询测试推送状态", crud = LogCrudEnum.R)
    @PostMapping("/test/status")
    public PushTestRunAdminVo testStatus(@Valid @RequestBody PushTestStatusReqVo reqVo) {
        return pushTestAdminBiz.status(reqVo);
    }

    @LogNoRet
    @FaLogOpr(value = "查询近 24 小时测试推送记录", crud = LogCrudEnum.R)
    @PostMapping("/test/recent")
    public List<PushTestRunAdminVo> recentTestRuns() {
        return pushTestAdminBiz.recent();
    }
}
