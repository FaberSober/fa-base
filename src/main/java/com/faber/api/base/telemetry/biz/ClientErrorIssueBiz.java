package com.faber.api.base.telemetry.biz;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.faber.api.base.telemetry.entity.ClientErrorIssue;
import com.faber.api.base.telemetry.enums.TelemetryIssueStatusEnum;
import com.faber.api.base.telemetry.mapper.ClientErrorIssueMapper;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;

import java.util.Date;

/** 客户端异常 Issue 查询与状态维护。 */
@Service
public class ClientErrorIssueBiz extends BaseBiz<ClientErrorIssueMapper, ClientErrorIssue> {

    public void updateStatus(Long id, TelemetryIssueStatusEnum status) {
        baseMapper.update(null, new LambdaUpdateWrapper<ClientErrorIssue>()
                .eq(ClientErrorIssue::getId, id)
                .set(ClientErrorIssue::getStatus, status)
                .set(ClientErrorIssue::getUpdateTime, new Date()));
    }
}
