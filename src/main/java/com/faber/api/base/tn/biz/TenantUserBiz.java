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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
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
        normalizeAndValidate(entity);

        long count = lambdaQuery()
                .eq(TenantUser::getTenantId, entity.getTenantId())
                .eq(TenantUser::getUserId, entity.getUserId())
                .ne(StrUtil.isNotBlank(entity.getId()), TenantUser::getId, entity.getId())
                .count();
        if (count > 0) {
            throw new BuzzException("该用户已关联到当前租户");
        }
    }

    private void normalizeAndValidate(TenantUser entity) {
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
    }

    /**
     * 新增关联时优先恢复同一租户和用户的逻辑删除记录，避免唯一索引与逻辑删除冲突。
     */
    @Override
    public boolean save(TenantUser entity) {
        return restoreOrCreate(entity, false);
    }

    @Override
    public boolean saveBatch(Collection<TenantUser> entityList) {
        if (CollUtil.isEmpty(entityList)) {
            return true;
        }
        for (TenantUser entity : entityList) {
            if (!save(entity)) {
                return false;
            }
        }
        return true;
    }

    private boolean restoreOrCreate(TenantUser entity, boolean tolerateActive) {
        normalizeAndValidate(entity);

        TenantUser existing = baseMapper.selectByTenantIdAndUserIdIgnoreLogic(
                entity.getTenantId(), entity.getUserId()
        );
        if (existing == null) {
            return super.save(entity);
        }

        if (Boolean.TRUE.equals(existing.getDeleted())) {
            return restore(existing, entity);
        }

        if (tolerateActive) {
            if (!Boolean.TRUE.equals(existing.getStatus())) {
                existing.setStatus(true);
                updateIgnoreLogic(existing);
            }
            return true;
        }

        throw new BuzzException("该用户已关联到当前租户");
    }

    private boolean restore(TenantUser existing, TenantUser source) {
        existing.setIsAdmin(source.getIsAdmin() == null ? false : source.getIsAdmin());
        existing.setStatus(source.getStatus() == null ? true : source.getStatus());
        existing.setSort(source.getSort() == null ? 0 : source.getSort());
        existing.setDescription(source.getDescription());
        existing.setDeleted(false);

        boolean updated = updateIgnoreLogic(existing);
        if (updated) {
            source.setId(existing.getId());
            source.setIsAdmin(existing.getIsAdmin());
            source.setStatus(existing.getStatus());
            source.setSort(existing.getSort());
            source.setDeleted(false);
        }
        return updated;
    }

    private boolean updateIgnoreLogic(TenantUser entity) {
        boolean updated = baseMapper.updateByIdIgnoreLogic(entity) > 0;
        if (updated) {
            afterUpdate(entity);
            afterChange(entity);
        }
        return updated;
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

    public List<TenantUser> getUserTenants(String userId) {
        if (isSuperAdminUser(userId)) {
            return tenantBiz.lambdaQuery()
                    .eq(Tenant::getStatus, true)
                    .orderByAsc(Tenant::getSort)
                    .orderByAsc(Tenant::getId)
                    .list()
                    .stream()
                    .filter(this::isTenantAvailable)
                    .map(tenant -> {
                        TenantUser item = new TenantUser();
                        item.setId(tenant.getId());
                        item.setTenantId(tenant.getId());
                        item.setTenantName(tenant.getName());
                        item.setUserId(userId);
                        item.setIsAdmin(true);
                        item.setStatus(true);
                        item.setSort(tenant.getSort());
                        item.setDescription(tenant.getDescription());
                        return item;
                    })
                    .collect(Collectors.toList());
        }

        List<TenantUser> list = lambdaQuery()
                .eq(TenantUser::getUserId, userId)
                .eq(TenantUser::getStatus, true)
                .orderByAsc(TenantUser::getSort)
                .orderByAsc(TenantUser::getId)
                .list();
        Map<String, Tenant> tenantMap = tenantBiz.getByIds(list.stream()
                        .map(TenantUser::getTenantId)
                        .filter(StrUtil::isNotBlank)
                        .distinct()
                        .collect(Collectors.toList()))
                .stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Tenant::getId, item -> item, (a, b) -> a));
        list = list.stream()
                .filter(item -> isTenantAvailable(tenantMap.get(item.getTenantId())))
                .collect(Collectors.toList());
        decorateList(list);
        return list;
    }

    public String getDefaultTenantId(String userId) {
        List<TenantUser> list = getUserTenants(userId);
        if (CollUtil.isEmpty(list)) {
            return null;
        }
        return list.get(0).getTenantId();
    }

    public boolean hasUserTenant(String userId, String tenantId) {
        if (StrUtil.hasBlank(userId, tenantId)) {
            return false;
        }
        if (!isTenantAvailable(tenantBiz.getById(tenantId))) {
            return false;
        }
        if (isSuperAdminUser(userId)) {
            return true;
        }
        return lambdaQuery()
                .eq(TenantUser::getUserId, userId)
                .eq(TenantUser::getTenantId, tenantId)
                .eq(TenantUser::getStatus, true)
                .count() > 0;
    }

    public boolean isTenantAdminUser(String userId, String tenantId) {
        if (StrUtil.hasBlank(userId, tenantId)) {
            return false;
        }
        if (!hasUserTenant(userId, tenantId)) {
            return false;
        }
        if (isSuperAdminUser(userId)) {
            return true;
        }
        return lambdaQuery()
                .eq(TenantUser::getUserId, userId)
                .eq(TenantUser::getTenantId, tenantId)
                .eq(TenantUser::getIsAdmin, true)
                .eq(TenantUser::getStatus, true)
                .count() > 0;
    }

    public void bindUserTenantIfAbsent(String tenantId, String userId) {
        if (StrUtil.hasBlank(tenantId, userId)) {
            return;
        }

        TenantUser entity = new TenantUser();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setIsAdmin(false);
        entity.setStatus(true);
        entity.setSort(0);
        restoreOrCreate(entity, true);
    }

    /**
     * 确保租户创建人是租户管理员。
     */
    public void ensureTenantAdmin(String tenantId, String userId) {
        if (StrUtil.hasBlank(tenantId, userId)) {
            return;
        }

        TenantUser entity = new TenantUser();
        entity.setTenantId(tenantId);
        entity.setUserId(userId);
        entity.setIsAdmin(true);
        entity.setStatus(true);
        entity.setSort(0);
        normalizeAndValidate(entity);

        TenantUser existing = baseMapper.selectByTenantIdAndUserIdIgnoreLogic(tenantId, userId);
        if (existing == null) {
            super.save(entity);
            return;
        }
        if (Boolean.TRUE.equals(existing.getDeleted())) {
            restore(existing, entity);
            return;
        }

        if (!Boolean.TRUE.equals(existing.getIsAdmin()) || !Boolean.TRUE.equals(existing.getStatus())) {
            existing.setIsAdmin(true);
            existing.setStatus(true);
            updateIgnoreLogic(existing);
        }
    }

    public List<String> getUserIdsByTenantId(String tenantId) {
        if (StrUtil.isBlank(tenantId) || !isTenantAvailable(tenantBiz.getById(tenantId))) {
            return Collections.emptyList();
        }
        return lambdaQuery()
                .eq(TenantUser::getTenantId, tenantId)
                .eq(TenantUser::getStatus, true)
                .list()
                .stream()
                .map(TenantUser::getUserId)
                .filter(StrUtil::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isTenantAvailable(Tenant tenant) {
        return tenant != null
                && Boolean.TRUE.equals(tenant.getStatus())
                && (tenant.getExpireTime() == null || tenant.getExpireTime().after(new Date()));
    }

}
