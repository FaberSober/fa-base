package com.faber.api.base.tn.biz;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.biz.UserBiz;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.tn.entity.Tenant;
import com.faber.api.base.tn.entity.TenantUser;
import com.faber.api.base.tn.mapper.TenantUserMapper;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 租户用户关联
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class TenantUserBiz extends BaseBiz<TenantUserMapper, TenantUser> {

    @Resource
    private TenantBiz tenantBiz;

    @Resource
    private UserBiz userBiz;

    @Override
    protected void saveBefore(TenantUser entity) {
        entity.setTenantId(StrUtil.trim(entity.getTenantId()));
        entity.setUserId(StrUtil.trim(entity.getUserId()));
        entity.setDescription(StrUtil.trim(entity.getDescription()));
        if (entity.getSort() == null) {
            entity.setSort(0);
        }

        Tenant tenant = tenantBiz.getById(entity.getTenantId());
        if (tenant == null) {
            throw new BuzzException("租户不存在");
        }

        User user = userBiz.getById(entity.getUserId());
        if (user == null) {
            throw new BuzzException("用户不存在");
        }

        long count = lambdaQuery()
                .eq(TenantUser::getTenantId, entity.getTenantId())
                .eq(TenantUser::getUserId, entity.getUserId())
                .ne(StrUtil.isNotBlank(entity.getId()), TenantUser::getId, entity.getId())
                .count();
        if (count > 0) {
            throw new BuzzException("该用户已关联到当前租户");
        }
    }

    @Override
    public void decorateOne(TenantUser item) {
        if (item == null) {
            return;
        }
        decorateList(Collections.singletonList(item));
    }

    @Override
    public void decorateList(List<TenantUser> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }

        List<String> tenantIds = list.stream()
                .map(TenantUser::getTenantId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
        List<String> userIds = list.stream()
                .map(TenantUser::getUserId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());

        Map<String, String> tenantNameMap = tenantBiz.getByIds(tenantIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Tenant::getId, Tenant::getName, (a, b) -> a));
        Map<String, String> userNameMap = userBiz.getByIds(userIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        list.forEach(item -> {
            item.setTenantName(tenantNameMap.get(item.getTenantId()));
            item.setUserName(userNameMap.get(item.getUserId()));
        });
    }

}
