package com.faber.api.base.admin.vo.ret;

import com.faber.api.base.admin.enums.ConfigSysSafePasswordTypeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * 系统参数
 */
@Data
public class SystemConfigPo implements Serializable {

    /**
     * 网站标题
     * system:title
     */
    private String title;

    /**
     * 网站副标题
     * system:subTitle
     */
    private String subTitle;

    /**
     * 网站logo
     * system:logo
     */
    private String logo;

    /**
     * 网站带文字logo
     * system:portal:logoWithText
     */
    private String logoWithText;

    /**
     * [官网]地址
     * system:portal:link
     */
    private String portalLink;

    /**
     * [Web]redis web管理页面地址
     * 配置文件: fa.setting.url.phpRedisAdmin
     */
    private String phpRedisAdmin;

    /**
     * [Web]SocketIO服务地址
     * 配置文件: fa.setting.url.socketUrl
     */
    private String socketUrl;

    /**
     * [安全]是否开启验证码
     */
    private Boolean safeCaptchaOn;
    /**
     * [安全]是否开启注册
     */
    private Boolean safeRegistrationOn;
    /**
     * [安全]密码类型
     */
    private ConfigSysSafePasswordTypeEnum safePasswordType;
    /**
     * [安全]密码最小长度
     */
    private Integer safePasswordLenMin;
    /**
     * [安全]密码最大长度
     */
    private Integer safePasswordLenMax;

}
