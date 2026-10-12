package com.faber.api.portal.contact.rest;

import com.faber.api.portal.contact.biz.PortalContactInquiryBiz;
import com.faber.api.portal.contact.entity.PortalContactInquiry;
import com.faber.api.portal.contact.vo.PortalContactInquiryReqVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FaLogBiz("Portal-官网咨询")
@RestController
@RequestMapping("/api/portal/contact")
public class PortalContactController extends BaseResHandler {

    @Resource
    private PortalContactInquiryBiz inquiryBiz;

    @FaLogOpr(value = "提交咨询", crud = LogCrudEnum.C)
    @IgnoreUserToken
    @PostMapping("/inquiries")
    public Ret<Void> submit(@Valid @RequestBody PortalContactInquiryReqVo reqVo) {
        PortalContactInquiry inquiry = new PortalContactInquiry();
        BeanUtils.copyProperties(reqVo, inquiry);
        inquiryBiz.submit(inquiry);
        return ok();
    }
}
