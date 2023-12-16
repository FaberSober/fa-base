package com.faber.api.base.admin.rest;

import com.faber.core.annotation.FaLogBiz;
import com.faber.core.web.rest.BaseController;
import com.faber.api.base.admin.biz.AlertBiz;
import com.faber.api.base.admin.entity.Alert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BASE-告警信息
 *
 * @author xu.pengfei
 * @email faberxu@gmail.com
 * @date 2023-12-16 11:40:21
 */
@FaLogBiz("BASE-告警信息")
@RestController
@RequestMapping("/api/base/admin/alert")
public class AlertController extends BaseController<AlertBiz, Alert, Integer> {

}