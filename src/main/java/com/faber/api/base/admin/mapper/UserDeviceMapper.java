package com.faber.api.base.admin.mapper;

import com.faber.core.config.mybatis.base.FaBaseMapper;
import com.faber.api.base.admin.entity.UserDevice;
import com.faber.api.portal.auth.vo.PortalLoginDeviceRetVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * BASE-用户设备
 * 
 * @author xu.pengfei
 * @email faberxu@gmail.com
 * @date 2024-01-11 14:52:44
 */
public interface UserDeviceMapper extends FaBaseMapper<UserDevice> {

    List<PortalLoginDeviceRetVo> selectPortalLoginDevices(
            @Param("userId") String userId,
            @Param("clientType") String clientType,
            @Param("currentDeviceId") String currentDeviceId
    );

}
