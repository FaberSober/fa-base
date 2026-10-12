package com.faber.api.base.msg.biz;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.msg.entity.Msg;
import com.faber.api.base.msg.enums.MsgTypeEnum;
import com.faber.api.base.msg.mapper.MsgMapper;
import com.faber.api.base.msg.vo.MsgStatisticVO;
import com.faber.api.base.msg.vo.MsgTenantStatisticVO;
import com.faber.api.base.msg.vo.MsgTenantUnreadCountVO;
import com.faber.api.base.tn.biz.TenantUserBiz;
import com.faber.api.base.tn.entity.TenantUser;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统-消息
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2020-12-13 21:19:53
 */
@Service
public class MsgBiz extends BaseBiz<MsgMapper, Msg> {

    @Resource
    UserBiz userBiz;

    @Resource
    private TenantUserBiz tenantUserBiz;

    @Override
    public boolean save(Msg entity) {
        if (entity != null) {
            entity.setTenantId(isTenantEnabled() ? getCurrentTenantId() : null);
        }
        return super.save(entity);
    }

    @Override
    public boolean saveBatch(Collection<Msg> entityList) {
        if (entityList != null) {
            entityList.forEach(entity -> entity.setTenantId(isTenantEnabled() ? getCurrentTenantId() : null));
        }
        return super.saveBatch(entityList);
    }

    @Override
    public boolean saveBatch(Collection<Msg> entityList, int batchSize) {
        return saveBatch(entityList);
    }

    @Override
    public QueryWrapper<Msg> parseQuery(QueryParams query) {
        QueryWrapper<Msg> wrapper = super.parseQuery(query);
        appendCurrentTenantScope(wrapper);
        return wrapper;
    }

    @Override
    public void decorateOne(Msg i) {
        User fromUser = userBiz.getByIdWithCache(i.getFromUserId());
        if (fromUser!= null) {
            i.setFromUserName(fromUser.getName());
        }
        i.setFromUser(fromUser);
    }

    /**
     * 消息数量统计。
     * 1. 未读消息数量；
     */
    public MsgStatisticVO countMine() {
        // 1. 未读消息数量
        long unreadCount = countUnread(null);
        long systemUnreadCount = countUnread(MsgTypeEnum.SYSTEM);
        long flowUnreadCount = countUnread(MsgTypeEnum.FLOW);

        MsgStatisticVO vo = new MsgStatisticVO();
        vo.setUnreadCount(unreadCount);
        vo.setSystemUnreadCount(systemUnreadCount);
        vo.setFlowUnreadCount(flowUnreadCount);
        return vo;
    }

    public MsgTenantStatisticVO countMineByTenant() {
        List<TenantUser> tenants = tenantUserBiz.getUserTenants(getCurrentUserId());
        List<String> tenantIds = tenants.stream().map(TenantUser::getTenantId).collect(Collectors.toList());
        Map<String, Long> tenantUnreadCounts = new LinkedHashMap<>();
        tenants.forEach(tenant -> tenantUnreadCounts.put(tenant.getTenantId(), 0L));

        if (!tenantIds.isEmpty()) {
            List<MsgTenantUnreadCountVO> counts = baseMapper.countUnreadByTenantIds(getCurrentUserId(), tenantIds);
            counts.forEach(item -> tenantUnreadCounts.put(item.getTenantId(), item.getUnreadCount()));
        }

        long unassignedUnreadCount = baseMapper.selectCount(
                new LambdaQueryWrapper<Msg>()
                        .eq(Msg::getToUserId, getCurrentUserId())
                        .eq(Msg::getIsRead, false)
                        .isNull(Msg::getTenantId)
        );
        long totalUnreadCount = tenantUnreadCounts.values().stream().mapToLong(Long::longValue).sum()
                + unassignedUnreadCount;

        MsgTenantStatisticVO vo = new MsgTenantStatisticVO();
        vo.setTenantUnreadCounts(tenantUnreadCounts);
        vo.setTotalUnreadCount(totalUnreadCount);
        return vo;
    }

    private long countUnread(MsgTypeEnum type) {
        LambdaQueryWrapper<Msg> wrapper = new LambdaQueryWrapper<Msg>()
                .eq(Msg::getToUserId, getCurrentUserId())
                .eq(Msg::getIsRead, false);
        if (type != null) {
            wrapper.eq(Msg::getType, type);
        }
        appendCurrentTenantScope(wrapper);
        return baseMapper.selectCount(wrapper);
    }

    private void appendCurrentTenantScope(QueryWrapper<Msg> wrapper) {
        String tenantId = getCurrentTenantId();
        if (isTenantEnabled() && StrUtil.isNotBlank(tenantId)) {
            wrapper.and(item -> item.eq("tenant_id", tenantId).or().isNull("tenant_id"));
        }
    }

    private void appendCurrentTenantScope(LambdaQueryWrapper<Msg> wrapper) {
        String tenantId = getCurrentTenantId();
        if (isTenantEnabled() && StrUtil.isNotBlank(tenantId)) {
            wrapper.and(item -> item.eq(Msg::getTenantId, tenantId).or().isNull(Msg::getTenantId));
        }
    }

    private void appendCurrentTenantScope(LambdaUpdateWrapper<Msg> wrapper) {
        String tenantId = getCurrentTenantId();
        if (isTenantEnabled() && StrUtil.isNotBlank(tenantId)) {
            wrapper.and(item -> item.eq(Msg::getTenantId, tenantId).or().isNull(Msg::getTenantId));
        }
    }

    /**
     * 消息批量已读
     * @param ids
     */
    public void batchRead(List<Long> ids) {
        Date now = new Date();
        LambdaUpdateWrapper<Msg> wrapper = new LambdaUpdateWrapper<Msg>()
                .in(Msg::getId, ids)
                .eq(Msg::getToUserId, getCurrentUserId())
                .set(Msg::getIsRead, true)
                .set(Msg::getReadTime, now);
        appendCurrentTenantScope(wrapper);
        baseMapper.update(null, wrapper);
    }

    /**
     * 全部已读
     */
    public void readAll() {
        Date now = new Date();
        LambdaUpdateWrapper<Msg> wrapper = new LambdaUpdateWrapper<Msg>()
                .eq(Msg::getToUserId, getCurrentUserId())
                .ne(Msg::getIsRead, true)
                .set(Msg::getIsRead, true)
                .set(Msg::getReadTime, now);
        appendCurrentTenantScope(wrapper);
        baseMapper.update(null, wrapper);
    }

    public void sendMsg(String fromUserId, String toUserId, String content) {
        Msg bean = new Msg();
        bean.setFromUserId(fromUserId);
        bean.setToUserId(toUserId);
        bean.setContent(content);
        bean.setIsRead(true);

        this.save(bean);
    }

}
