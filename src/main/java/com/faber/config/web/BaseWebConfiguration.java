package com.faber.config.web;

import cn.hutool.core.collection.ListUtil;
import com.faber.config.interceptor.*;
import com.faber.core.config.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

/**
 * web配置
 * @author Farando
 * @date 2023/1/13 23:16
 * @description
 */
public abstract class BaseWebConfiguration extends WebMvcConfigurationSupport {

    @Value("${spring.jackson.date-format}")
    private String dateFormatPattern;

    @Value("${spring.jackson.time-zone}")
    private String timeZone;

    @Bean
    GlobalExceptionHandler getGlobalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Override
    protected void addResourceHandlers(ResourceHandlerRegistry registry) {
        //默认的资源映射需要填写，不然不能正常访问
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
        //调用基类的方法
        super.addResourceHandlers(registry);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
    }

    @Bean
    UserAuthRestInterceptor getUserAuthRestInterceptor() {
        return new UserAuthRestInterceptor();
    }

    @Bean
    PermissionInterceptor getPermissionInterceptor() {
        return new PermissionInterceptor();
    }

    @Bean
    ApiTokenInterceptor getApiTokenInterceptor() {
        return new ApiTokenInterceptor();
    }

    @Bean
    GateLogInterceptor getGateLogInterceptor() {
        return new GateLogInterceptor();
    }

    @Bean
    FirstEmptyInterceptor getFirstEmptyInterceptor() {
        return new FirstEmptyInterceptor();
    }

    /**
     * SpringBoot2 版本中spring.jackson.date-format设置以后不生效的速效解决方法
     * https://blog.csdn.net/u012581020/article/details/105955060
     *
     * @param converters
     */
    @Override
    protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper objectMapper = converter.getObjectMapper();
        // 生成JSON时,将所有Long转换成String
        // 因为js中得数字类型不能包含所有的java long值
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Long.class, ToStringSerializer.instance);
        simpleModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
        objectMapper.registerModule(simpleModule);
        // 时间格式化
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //这个可以引用spring boot yml 里的格式化配置和时区配置
        objectMapper.setDateFormat(new SimpleDateFormat(dateFormatPattern));
        objectMapper.setTimeZone(TimeZone.getTimeZone(timeZone));
        // 设置格式化内容
        converter.setObjectMapper(objectMapper);
        converters.add(0, converter);
        super.extendMessageConverters(converters);
    }

}
