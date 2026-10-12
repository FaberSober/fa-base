package com.faber.api.base.tn.mapper;

import com.faber.api.base.tn.entity.TenantPermission;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;

public interface TenantPermissionMapper extends FaBaseMapper<TenantPermission> {

    TenantPermission selectByTenantIdAndMenuIdIgnoreLogic(
            @Param("tenantId") String tenantId,
            @Param("menuId") Long menuId
    );
}
