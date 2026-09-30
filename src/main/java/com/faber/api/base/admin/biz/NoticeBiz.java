package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.Notice;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.mapper.NoticeMapper;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.api.base.msg.helper.MsgHelper;
import com.faber.api.base.msg.helper.config.MsgSendSysConfig;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * BASE-通知与公告
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2021-01-07 09:37:36
 */
@Service
public class NoticeBiz extends BaseBiz<NoticeMapper, Notice> {

    @Resource
    private UserBiz userBiz;

    @Resource
    private MsgHelper msgHelper;

    @Resource
    private TenantUserBiz tenantUserBiz;

    @Autowired
    private Executor executor;

    @Override
    protected void afterSave(Notice entity) {
        Map<String, Object> holdMap = new HashMap<>(BaseContextHandler.getHoldMap());
        String fromUserId = getCurrentUserId();
        boolean tenantEnabled = isTenantEnabled();
        String tenantId = entity.getTenantId();
        Runnable send = () -> executor.execute(() -> {
            try {
                BaseContextHandler.setHoldMap(holdMap);
                BaseContextHandler.setTenantId(tenantEnabled ? tenantId : null);
                List<String> userIds = tenantEnabled
                        ? tenantUserBiz.getUserIdsByTenantId(tenantId)
                        : userBiz.lambdaQuery().select(User::getId).list().stream()
                                .map(User::getId).collect(Collectors.toList());

                MsgSendSysConfig config = MsgSendSysConfig.builder()
                        .buzzId(entity.getId() + "")
                        .content(entity.getTitle() + ": " + entity.getContent())
                        .build();
                msgHelper.sendSysMsg(fromUserId, userIds.toArray(new String[0]), config);
            } finally {
                BaseContextHandler.remove();
            }
        });
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send.run();
                }
            });
        } else {
            send.run();
        }
    }

}
