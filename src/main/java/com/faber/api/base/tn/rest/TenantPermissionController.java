package com.faber.api.base.tn.rest;

import com.faber.api.base.tn.biz.TenantPermissionBiz;
import com.faber.api.base.tn.vo.req.TenantPermissionUpdateVo;
import com.faber.api.base.tn.vo.ret.TenantPermissionScopeVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@FaLogBiz("租户权限")
@RestController
@RequestMapping("/api/base/tn/tenantPermission")
public class TenantPermissionController extends BaseResHandler {

    @Resource
    private TenantPermissionBiz tenantPermissionBiz;

    @FaLogOpr("获取租户权限")
    @GetMapping("/getMenuIds/{tenantId}")
    public Ret<List<Long>> getMenuIds(@PathVariable String tenantId) {
        return ok(tenantPermissionBiz.getMenuIds(tenantId));
    }

    @FaLogOpr("获取租户权限范围")
    @GetMapping("/getPermissionScope/{tenantId}")
    public Ret<TenantPermissionScopeVo> getPermissionScope(@PathVariable String tenantId) {
        return ok(tenantPermissionBiz.getPermissionScope(tenantId));
    }

    @FaLogOpr("更新租户权限")
    @PostMapping("/updateMenuIds")
    public Ret<Boolean> updateMenuIds(@Valid @RequestBody TenantPermissionUpdateVo vo) {
        tenantPermissionBiz.updateMenuIds(vo);
        return ok();
    }
}
