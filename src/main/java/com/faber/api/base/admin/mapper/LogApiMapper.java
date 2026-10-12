package com.faber.api.base.admin.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import com.faber.api.base.admin.entity.LogApi;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LogApiMapper extends FaBaseMapper<LogApi> {

    // 添加拦截忽略注解，指定忽略全表删除拦截器
    @InterceptorIgnore(blockAttack = "true")
    int deleteAll();

    Page<LogApi> selectPageFromTables(
            Page<LogApi> page,
            @Param("tables") List<String> tables,
            @Param(Constants.WRAPPER) Wrapper<LogApi> wrapper
    );

}
