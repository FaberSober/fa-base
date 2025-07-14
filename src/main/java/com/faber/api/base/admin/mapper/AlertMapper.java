package com.faber.api.base.admin.mapper;

import com.faber.api.base.admin.vo.ret.MaterialVo;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import com.faber.api.base.admin.entity.Alert;

import java.util.List;

/**
 * BASE-告警信息
 * 
 * @author xu.pengfei
 * @email faberxu@gmail.com
 * @date 2023-12-16 11:40:20
 */
public interface AlertMapper extends FaBaseMapper<Alert> {


    /**
     * 库存不足信息
     * @return
     */
    List<MaterialVo> checkInventoryShortageAlerts();
	
}
