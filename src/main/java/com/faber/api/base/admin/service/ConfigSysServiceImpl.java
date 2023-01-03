package com.faber.api.base.admin.service;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.biz.ConfigSysBiz;
import com.faber.api.base.admin.entity.ConfigSys;
import com.faber.core.service.ConfigSysService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
public class ConfigSysServiceImpl implements ConfigSysService {

    @Resource
    ConfigSysBiz configSysBiz;

    @Override
    public String getStoreLocalPath() throws IOException {
        ConfigSys configSys = configSysBiz.getOne();
        if (configSys == null || configSys.getData() == null || StrUtil.isEmpty(configSys.getData().getStoreLocalPath())) {
            return ConfigSysService.super.getStoreLocalPath();
        }
        return configSys.getData().getStoreLocalPath();
    }

}
