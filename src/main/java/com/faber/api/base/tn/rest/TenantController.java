package com.faber.api.base.tn.rest;

import com.faber.api.base.tn.biz.TenantBiz;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.vo.req.TenantWithPermissionsReq;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.vo.msg.Ret;
import com.faber.core.web.rest.BaseController;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FaLogBiz("租户")
@RestController
@RequestMapping("/api/base/tn/tenant")
public class TenantController extends BaseController<TenantBiz, Tenant, String> {

    @FaLogOpr("带权限创建租户")
    @PostMapping("/createWithPermissions")
    public Ret<Tenant> createWithPermissions(@Valid @RequestBody TenantWithPermissionsReq request) {
        return ok(baseBiz.createWithPermissions(request.getTenant(), request.getMenuIds()));
    }

    @FaLogOpr("带权限更新租户")
    @PostMapping("/updateWithPermissions")
    public Ret<Tenant> updateWithPermissions(@Valid @RequestBody TenantWithPermissionsReq request) {
        return ok(baseBiz.updateWithPermissions(request.getTenant(), request.getMenuIds()));
    }
}
