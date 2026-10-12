package com.faber.api.base.telemetry.biz;

import com.faber.api.base.telemetry.entity.ClientErrorEvent;
import com.faber.api.base.telemetry.mapper.ClientErrorEventMapper;
import com.faber.core.web.biz.BaseBiz;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;

/** 客户端异常事件查询。 */
@Service
public class ClientErrorEventBiz extends BaseBiz<ClientErrorEventMapper, ClientErrorEvent> {

    public List<ClientErrorEvent> listRecentByIssueId(Long issueId) {
        return list(new LambdaQueryWrapper<ClientErrorEvent>()
                .eq(ClientErrorEvent::getIssueId, issueId)
                .orderByDesc(ClientErrorEvent::getOccurTime)
                .last("limit 10"));
    }
}
