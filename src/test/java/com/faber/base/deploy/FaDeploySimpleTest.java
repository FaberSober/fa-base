package com.faber.base.deploy;

import com.faber.core.utils.FaDeployHelper;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Farando
 * @date 2023/2/5 16:54
 * @description
 */
public class FaDeploySimpleTest {

    @Test
    public void testDeploy() throws JSchException, SftpException, IOException {
        FaDeployHelper helper = new FaDeployHelper();
        helper.setHost("127.0.0.1");
        helper.setUsername("root");
        helper.setPassword("123456");

        helper.setJarPath("/opt/fa-admin/"); // 远程部署jar目录
        helper.setNginxHtmlPath("/etc/nginx/html/fa-admin"); // 远程部署前端dist目录

        // 打包
        helper.mvnPackage();
        // 部署前端
        helper.deployFrontend();
        // 部署Jar
        helper.deployJar();
    }

}
