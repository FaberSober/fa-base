package com.faber.api.base.tn.mapper;

import com.faber.api.base.tn.entity.Tenant;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;

public interface TenantMapper extends FaBaseMapper<Tenant> {

    Tenant selectByCodeIgnoreLogic(@Param("code") String code);
}
