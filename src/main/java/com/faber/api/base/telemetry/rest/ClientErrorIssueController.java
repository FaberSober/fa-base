package com.faber.api.base.telemetry.rest;

import com.faber.api.base.telemetry.biz.ClientErrorIssueBiz;
import com.faber.api.base.telemetry.entity.ClientErrorIssue;
import com.faber.api.base.telemetry.enums.TelemetryIssueStatusEnum;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.QueryParams;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 客户端异常 Issue 查询与状态维护。 */
@FaLogBiz("客户端异常 Issue")
@RestController
@RequestMapping("/api/base/telemetry/issue")
public class ClientErrorIssueController extends BaseResHandler {

    private final ClientErrorIssueBiz issueBiz;

    public ClientErrorIssueController(ClientErrorIssueBiz issueBiz) {
        this.issueBiz = issueBiz;
    }

    @PostMapping("/page")
    public TableRet<ClientErrorIssue> page(@RequestBody QueryParams query) {
        return issueBiz.selectPageByQuery(query);
    }

    @GetMapping("/getDetail/{id}")
    public Ret<ClientErrorIssue> getDetail(@PathVariable Long id) {
        return ok(issueBiz.getDetailById(id));
    }

    @FaLogOpr(value = "更新异常 Issue 状态", crud = LogCrudEnum.U)
    @PostMapping("/status/{id}")
    public Ret<Void> updateStatus(@PathVariable Long id, @RequestParam TelemetryIssueStatusEnum status) {
        issueBiz.updateStatus(id, status);
        return ok();
    }
}
