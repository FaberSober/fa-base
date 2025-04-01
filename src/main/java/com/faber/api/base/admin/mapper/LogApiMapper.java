package com.faber.api.base.admin.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import com.faber.api.base.admin.entity.LogApi;

public interface LogApiMapper extends FaBaseMapper<LogApi> {

    // 添加拦截忽略注解，指定忽略全表删除拦截器
    @InterceptorIgnore(blockAttack = "true")
    int deleteAll();

}
