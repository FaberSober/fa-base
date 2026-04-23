package com.faber.api.base.tn.rest;

import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.api.base.tn.entity.TenantUser;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.web.rest.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FaLogBiz("租户用户关联")
@RestController
@RequestMapping("/api/base/tn/tenantUser")
public class TenantUserController extends BaseController<TenantUserBiz, TenantUser, String> {
}
