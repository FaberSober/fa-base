package com.faber.api.portal.contacts.rest;

import com.faber.api.portal.contacts.biz.PortalContactsBiz;
import com.faber.api.portal.contacts.vo.PortalDepartmentNodeVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@FaLogBiz("Portal-联系人")
@RestController
@RequestMapping("/api/portal/contacts")
public class PortalContactsController extends BaseResHandler {

    @Resource
    private PortalContactsBiz contactsBiz;

    @FaLogOpr("联系人部门树")
    @LogNoRet
    @GetMapping("/departments/tree")
    public Ret<List<PortalDepartmentNodeVo>> departmentTree() {
        return ok(contactsBiz.getDepartmentTree());
    }
}
