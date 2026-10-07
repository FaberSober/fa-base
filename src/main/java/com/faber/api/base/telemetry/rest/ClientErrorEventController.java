package com.faber.api.base.telemetry.rest;

import com.faber.api.base.telemetry.biz.ClientErrorEventBiz;
import com.faber.api.base.telemetry.entity.ClientErrorEvent;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.QueryParams;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 客户端异常事件查询。 */
@FaLogBiz("客户端异常事件")
@RestController
@RequestMapping("/api/base/telemetry/errorEvent")
public class ClientErrorEventController extends BaseResHandler {

    private final ClientErrorEventBiz errorEventBiz;

    public ClientErrorEventController(ClientErrorEventBiz errorEventBiz) {
        this.errorEventBiz = errorEventBiz;
    }

    @FaLogOpr(value = "分页查询客户端异常事件", crud = LogCrudEnum.R)
    @PostMapping("/page")
    public TableRet<ClientErrorEvent> page(@RequestBody QueryParams query) {
        return errorEventBiz.selectPageByQuery(query);
    }

    @FaLogOpr(value = "查询客户端异常事件详情", crud = LogCrudEnum.R)
    @GetMapping("/getDetail/{id}")
    public Ret<ClientErrorEvent> getDetail(@PathVariable Long id) {
        return ok(errorEventBiz.getDetailById(id));
    }

    @FaLogOpr(value = "查询 Issue 最近事件", crud = LogCrudEnum.R)
    @GetMapping("/recentByIssue/{issueId}")
    public Ret<List<ClientErrorEvent>> recentByIssue(@PathVariable Long issueId) {
        return ok(errorEventBiz.listRecentByIssueId(issueId));
    }
}
