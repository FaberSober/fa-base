package com.faber.api.base.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.faber.api.base.admin.enums.ConfigSysSafePasswordTypeEnum;
import com.faber.core.annotation.FaModalName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.bean.BaseDelEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;


/**
 * BASE-配置-系统配置
 * @author xu.pengfei
 * @email faberxu@gmail.com
 * @date 2022/12/12 10:07
 */
@Data
@ToString
@FaModalName(name = "配置-系统配置")
@TableName(value = "base_config_sys", autoResultMap = true)
public class ConfigSys extends BaseDelEntity {

    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 配置JSON */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Config data;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Config {
        /**
         * 网站标题
         */
        private String title;
        /**
         * 网站标题-颜色
         */
        private String titleColor;
        /**
         * 网站副标题
         */
        private String subTitle;
        /**
         * 网站副标题-颜色
         */
        private String subTitleColor;
        /**
         * 版权信息-颜色
         */
        private String cop;
        /**
         * 版权信息
         */
        private String copColor;
        /**
         * 登录背景图
         */
        private String loginBg;
        /**
         * 登录页面样式
         */
        private String loginPageType;
        /**
         * 顶部菜单条样式
         */
        private String topMenuBarStyle;
        /**
         * 网站logo
         */
        private String logo;
        /**
         * 网站带文字logo
         */
        private String logoWithText;
        /**
         * [官网]地址
         */
        private String portalLink;
        /**
         * [安全]是否开启验证码
         */
        private Boolean safeCaptchaOn = true;
        /**
         * [安全]是否开启注册
         */
        private Boolean safeRegistrationOn = true;
        /**
         * [安全]密码类型
         */
        private ConfigSysSafePasswordTypeEnum safePasswordType = ConfigSysSafePasswordTypeEnum.NUM;
        /**
         * [安全]密码最小长度
         */
        private Integer safePasswordLenMin = 1;
        /**
         * [安全]密码最大长度
         */
        private Integer safePasswordLenMax = 30;
        /**
         * [安全]token有效时长(小时)
         */
        private Integer safeTokenExpireHour = 24;
        /**
         * [存储][本地]目录
         */
        private String storeLocalPath;
    }

}
