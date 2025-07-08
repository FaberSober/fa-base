package com.faber.api.base.admin.rest;

import com.faber.api.base.admin.biz.DictDataBiz;
import com.faber.api.base.admin.entity.DictData;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.web.rest.BaseTreeController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BASE-字典值
 *
 * @author xu.pengfei
 * @email 1508075252@qq.com
 * @date 2025-07-08 17:22:52
 */
@FaLogBiz("BASE-字典值")
@RestController
@RequestMapping("/api/base/admin/dictData")
public class DictDataController extends BaseTreeController<DictDataBiz, DictData, Integer> {

}
