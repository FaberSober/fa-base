package com.faber.api.base.admin.rest;

import com.faber.api.base.admin.biz.SystemUpdateLogBiz;
import com.faber.api.base.admin.entity.SystemUpdateLog;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.config.annotation.Permission;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.QueryParams;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.io.IOException;

@Permission(permission = "/admin/system/base/systemUpdateLog")
@FaLogBiz("系统更新")
@RestController
@RequestMapping("/api/base/admin/systemUpdateLog")
public class SystemUpdateLogController extends BaseResHandler {

    @Resource
    private SystemUpdateLogBiz systemUpdateLogBiz;

    @LogNoRet
    @PostMapping("/page")
    public TableRet<SystemUpdateLog> page(@RequestBody QueryParams query) {
        return systemUpdateLogBiz.selectPageByQuery(query);
    }

    @LogNoRet
    @GetMapping("/getDetail/{id}")
    public Ret<SystemUpdateLog> getDetail(@PathVariable Integer id) {
        return ok(systemUpdateLogBiz.getDetailById(id));
    }

    @LogNoRet
    @PostMapping("/exportExcel")
    public void exportExcel(@RequestBody QueryParams query) throws IOException {
        systemUpdateLogBiz.exportExcel(query);
    }

}
