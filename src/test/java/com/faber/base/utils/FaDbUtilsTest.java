package com.faber.base.utils;

import com.faber.FaTestApp;
import com.faber.api.base.rbac.biz.RbacMenuBiz;
import com.faber.api.base.rbac.entity.RbacMenu;
import com.faber.core.utils.FaDbUtils;
import com.github.pagehelper.PageInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author xupengfei
 * @email faberxu@gmail.com
 * @date 2022/12/8 11:39
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {FaTestApp.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FaDbUtilsTest {

    @Resource
    RbacMenuBiz rbacMenuBiz;

    @Test
    public void testLoopPage() {
        FaDbUtils.loopPage(
                rbacMenuBiz.lambdaQuery(),
                pageInfo -> {
                    log.debug("page: {}, isHasNextPage: {}, size: {}, pageSize: {}, total: {}", pageInfo.getPageNum(), pageInfo.isHasNextPage(), pageInfo.getSize(), pageInfo.getPageSize(), pageInfo.getTotal());
                    List<RbacMenu> list = pageInfo.getList();
                    for (RbacMenu menu : list) {
                        log.debug(menu.toString());
                    }
                },
                10
        );
    }

}
