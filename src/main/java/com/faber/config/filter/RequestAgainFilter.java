package com.faber.config.filter;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.faber.api.base.admin.biz.LogApiBiz;
import com.faber.api.base.admin.entity.LogApi;
import com.faber.core.config.filter.wrapper.BodyHttpServletRequestWrapper;
import com.faber.core.config.filter.wrapper.BodyHttpServletResponseWrapper;
import com.faber.core.constant.CommonConstants;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.IpUtils;
import com.faber.core.vo.utils.IpAddr;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 请求重复读取日志记录Filter
 * 1. 包装request，使得请求内容可以多次读取；
 * 2. 包装response，可以读取返回内容；
 * 3. 记录请求日志；
 * 4. 销毁上下文中记录的登录用户信息；
 *
 * @author xu.pengfei
 * @date 2022/11/28 11:39
 */
@Slf4j
@WebFilter(filterName = "RequestAgainFilter", urlPatterns = "/api/*", asyncSupported = true)
public class RequestAgainFilter implements Filter {

    /**
     * 列表的api不记录日志
     * TODO 这里要支持写入配置文件中
     */
    private static final List<String> NO_LOG_APIS = Arrays.asList("/api/admin/logApi/page", "/api/admin/logLogin/page", "/api/admin/dict/getSystemConfig");
    private static final Set<String> SKIP_URLS = new HashSet<>();

    public static void addSkipUrl(String url) {
        SKIP_URLS.add(url);
    }

    @Resource
    private LogApiBiz logApiBiz;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        long startTime = System.currentTimeMillis();

        if (SKIP_URLS.contains(servletRequest.getServletContext().getContextPath() + ((HttpServletRequest) servletRequest).getRequestURI())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        BodyHttpServletRequestWrapper requestWrapper = new BodyHttpServletRequestWrapper((HttpServletRequest) servletRequest);
        BodyHttpServletResponseWrapper responseWrapper = new BodyHttpServletResponseWrapper((HttpServletResponse) servletResponse);

        // 交给下一个过滤器或servlet处理
        filterChain.doFilter(requestWrapper, responseWrapper);

        byte[] content = responseWrapper.getContent();
        //获取response的值
        String responseData = IOUtils.toString(content, "UTF-8");

        //注意 此处是servletResponse 不是responseWrapper,写responseWrapper的话 依旧响应不了
        ServletOutputStream outputStream = servletResponse.getOutputStream();
        outputStream.write(content);
        outputStream.flush();
        outputStream.close();

        if (NO_LOG_APIS.contains(requestWrapper.getRequestURI())) {
            BaseContextHandler.remove(); // 销毁上下文中记录的登录用户信息
            return;
        }

        String logNoRet = responseWrapper.getHeader("LogNoRet");
        LogApi logApi = new LogApi();

        logApi.setBiz(StrUtil.toString(requestWrapper.getAttribute("FaLogBiz")));
        logApi.setOpr(StrUtil.toString(requestWrapper.getAttribute("FaLogOpr")));
        logApi.setOprRemark(BaseContextHandler.getLogOprRemark());
        logApi.setCrud((LogCrudEnum) requestWrapper.getAttribute("FaLogCrud"));

        // request basic information
        String url = requestWrapper.getRequestURI();
        if (StrUtil.isNotEmpty(requestWrapper.getQueryString())) {
            url += "?" + requestWrapper.getQueryString();
        }
        logApi.setUrl(url);
        logApi.setMethod(requestWrapper.getMethod());
        logApi.setAgent(requestWrapper.getHeader("User-Agent"));

        // 解析agent字符串
        UserAgent ua = UserAgentUtil.parse(logApi.getAgent());
        logApi.setOs(ua.getOs().toString());
        logApi.setBrowser(ua.getBrowser().toString());
        logApi.setVersion(ua.getVersion());
        logApi.setMobile(ua.isMobile() ? true : false);

        // 自定义Header信息
        logApi.setFaFrom(requestWrapper.getHeader(CommonConstants.FA_FROM));
        try {
            if (StrUtil.isNotEmpty(requestWrapper.getHeader(CommonConstants.FA_VERSION_CODE))) {
                logApi.setVersionCode(Long.parseLong(requestWrapper.getHeader(CommonConstants.FA_VERSION_CODE)));
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        logApi.setVersionName(requestWrapper.getHeader(CommonConstants.FA_VERSION_NAME));

        logApi.setCrtHost(IpUtils.getRequestIp(requestWrapper));
        logApi.setRequest(requestWrapper.getBody());
        logApi.setReqSize(logApi.getRequest().length());

        logApi.setRetStatus(responseWrapper.getStatus());

        if ("1".equals(logNoRet) && !"200".equals(logApi.getRetStatus())) {
            logApi.setResponse("");
        } else {
            logApi.setResponse(responseData);
        }

        logApi.setRetSize(responseData.length());

        logApi.setDuration(System.currentTimeMillis() - startTime);

        // 获取IP地址
        IpAddr ipAddr = IpUtils.getIpAddrByApi(logApi.getCrtHost());
        if (ipAddr != null) {
            logApi.setPro(ipAddr.getPro());
            logApi.setCity(ipAddr.getCity());
            logApi.setAddr(ipAddr.getAddr());
        }

        // 取备注
        logApi.setRemark(BaseContextHandler.getLogRemark());

        logApiBiz.save(logApi);

        BaseContextHandler.remove(); // 销毁上下文中记录的登录用户信息
    }

    /**
     * 过滤器销毁
     * explain:在容器中销毁当前过滤器的时候自动调用
     */
    @Override
    public void destroy() {
        log.debug("销毁过滤器!");
    }
}
