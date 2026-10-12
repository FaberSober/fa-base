package com.faber.api.portal.contact.rest;

import com.faber.api.portal.contact.biz.PortalContactInquiryBiz;
import com.faber.api.portal.contact.entity.PortalContactInquiry;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.web.rest.BaseController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@FaLogBiz("Portal-官网咨询管理")
@RestController
@RequestMapping("/api/base/admin/portalContactInquiry")
public class PortalContactInquiryAdminController extends BaseController<PortalContactInquiryBiz, PortalContactInquiry, Long> {}
