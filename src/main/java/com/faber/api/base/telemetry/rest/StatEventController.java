package com.faber.api.base.telemetry.rest;

import com.faber.api.base.telemetry.biz.StatEventBiz;
import com.faber.api.base.telemetry.entity.StatEvent;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.QueryParams;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Telemetry 业务事件明细，仅作为统计排查入口。 */
@FaLogBiz("Telemetry 业务事件")
@RestController
@RequestMapping("/api/base/telemetry/statEvent")
public class StatEventController extends BaseResHandler {

    private final StatEventBiz statEventBiz;

    public StatEventController(StatEventBiz statEventBiz) {
        this.statEventBiz = statEventBiz;
    }

    @PostMapping("/page")
    public TableRet<StatEvent> page(@RequestBody QueryParams query) {
        return statEventBiz.selectPageByQuery(query);
    }

    @GetMapping("/getDetail/{id}")
    public Ret<StatEvent> getDetail(@PathVariable Long id) {
        return ok(statEventBiz.getById(id));
    }
}
