package com.faber.api.base.tn.mapper;

import com.faber.api.base.tn.entity.TenantUser;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;

public interface TenantUserMapper extends FaBaseMapper<TenantUser> {

    TenantUser selectByTenantIdAndUserIdIgnoreLogic(
            @Param("tenantId") String tenantId,
            @Param("userId") String userId
    );
}
