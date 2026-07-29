package com.faber.api.base.admin.mapper;

import com.faber.core.config.mybatis.base.FaBaseMapper;
import com.faber.api.base.admin.entity.SystemUpdateLog;
import org.apache.ibatis.annotations.Param;

/**
 * BASE-系统版本更新日志表
 * 
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-08-17 17:10:02
 */
public interface SystemUpdateLogMapper extends FaBaseMapper<SystemUpdateLog> {

    SystemUpdateLog getDetailById(@Param("id") Integer id);

}
