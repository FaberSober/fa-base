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
     * [Web]redis web管理页面地址
     */
    private String phpRedisAdmin;

    /**
     * [Web]SocketIO服务地址
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
