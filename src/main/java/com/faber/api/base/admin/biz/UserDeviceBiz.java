package com.faber.api.base.admin.biz;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.entity.User;
import com.faber.api.base.admin.entity.UserDevice;
import com.faber.api.base.admin.mapper.UserDeviceMapper;
import com.faber.core.constant.FaSetting;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * BASE-用户设备
 *
 * @author xu.pengfei
 * @email faberxu@gmail.com
 * @date 2024-01-11 14:52:44
 */
@Service
public class UserDeviceBiz extends BaseBiz<UserDeviceMapper,UserDevice> {

    @Resource
    private FaSetting faSetting;

    @Resource
    UserBiz userBiz;

    @Override
    public void decorateOne(UserDevice i) {
        User user = userBiz.getByIdWithCache(i.getUserId());
        if (user != null) {
            i.setUserName(user.getName());
        }
    }

    public void updateMine(UserDevice entity) {
        String currentUserId = getCurrentUserId();
        long count = lambdaQuery()
                .eq(UserDevice::getDeviceId, entity.getDeviceId())
                .isNull(UserDevice::getClientType)
                .count();

        if (count > 1) {
            throw new BuzzException("重复设备编码，请联系管理员");
        }

        if (count == 0) {
            long existingClientCount = lambdaQuery()
                    .eq(UserDevice::getDeviceId, entity.getDeviceId())
                    .count();
            if (existingClientCount > 0) {
                throw new BuzzException("设备已登记为客户端设备，不能通过旧设备接口修改");
            }
            entity.setUserId(currentUserId);
            entity.setClientType(null);
            entity.setEnable(faSetting.getApp().isDeviceDefaultAllow()); // 默认不允许访问
            entity.setLastOnlineTime(new Date());
            this.save(entity);
            return;
        }

        UserDevice entityDB = lambdaQuery()
                .eq(UserDevice::getDeviceId, entity.getDeviceId())
                .isNull(UserDevice::getClientType)
                .one();
        if (!currentUserId.equals(entityDB.getUserId())) {
            throw new BuzzException("设备已登记到其他用户，不允许转移归属");
        }
        entityDB.setModel(entity.getModel());
        entityDB.setManufacturer(entity.getManufacturer());
        entityDB.setOs(entity.getOs());
        entityDB.setOsVersion(entity.getOsVersion());
        this.updateById(entityDB);
    }

    /** 登录成功后按用户、客户端类型和客户端实例 ID 维护设备登记。 */
    public void registerClientOnLogin(User user, String clientType, String clientInstanceId, String os) {
        if (user == null || StrUtil.hasBlank(user.getId(), clientType, clientInstanceId)) return;

        UserDevice entity = lambdaQuery()
                .eq(UserDevice::getUserId, user.getId())
                .eq(UserDevice::getClientType, clientType)
                .eq(UserDevice::getDeviceId, clientInstanceId)
                .one();
        Date now = new Date();
        if (entity == null) {
            entity = new UserDevice();
            entity.setUserId(user.getId());
            entity.setClientType(clientType);
            entity.setDeviceId(clientInstanceId);
            entity.setEnable(faSetting.getApp().isDeviceDefaultAllow());
        }
        if (StrUtil.isNotBlank(os)) entity.setOs(os);
        entity.setLastOnlineTime(now);

        if (entity.getId() == null) save(entity);
        else updateById(entity);
    }

    public UserDevice getByDeviceId(String deviceId) {
        long count = lambdaQuery()
                .eq(UserDevice::getDeviceId, deviceId)
                .isNull(UserDevice::getClientType)
                .count();

        if (count > 1) {
            throw new BuzzException("重复设备编码，请联系管理员");
        }

        if (count == 0) {
            return null;
        }

        UserDevice entityDB = lambdaQuery()
                .eq(UserDevice::getDeviceId, deviceId)
                .isNull(UserDevice::getClientType)
                .one();
        return entityDB;
    }

    public void updateLastOnlineTime(int id) {
        lambdaUpdate()
                .set(UserDevice::getLastOnlineTime, LocalDateTime.now())
                .eq(UserDevice::getId, id)
                .update();
    }

}
