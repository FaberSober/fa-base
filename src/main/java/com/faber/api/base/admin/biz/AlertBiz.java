package com.faber.api.base.admin.biz;

import org.springframework.stereotype.Service;

import com.faber.api.base.admin.entity.Alert;
import com.faber.api.base.admin.mapper.AlertMapper;
import com.faber.core.web.biz.BaseBiz;

/**
 * BASE-告警信息
 *
 * @author xu.pengfei
 * @email faberxu@gmail.com
 * @date 2023-12-16 11:40:20
 */
@Service
public class AlertBiz extends BaseBiz<AlertMapper,Alert> {
}