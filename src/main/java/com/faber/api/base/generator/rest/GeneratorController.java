package com.faber.api.base.generator.rest;

import com.faber.api.base.generator.biz.GeneratorBiz;
import com.faber.api.base.generator.vo.req.CodeCopyVo;
import com.faber.api.base.generator.vo.req.CodeGenReqVo;
import com.faber.api.base.generator.vo.req.TableQueryVo;
import com.faber.api.base.generator.vo.ret.CodeGenRetVo;
import com.faber.api.base.generator.vo.ret.TableVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author Farando
 * @date 2023/3/9 10:51
 * @description
 */
@FaLogBiz("代码生成")
@RestController
@RequestMapping("/api/base/generator/generator")
public class GeneratorController extends BaseResHandler {

    @Resource
    GeneratorBiz generatorBiz;

    @FaLogOpr(value = "查询表", crud = LogCrudEnum.R)
    @LogNoRet
    @RequestMapping(value = "/pageTable", method = RequestMethod.POST)
    @ResponseBody
    public TableRet<TableVo> pageTable(@RequestBody BasePageQuery<TableQueryVo> query) {
        return generatorBiz.pageTable(query);
    }

    @FaLogOpr(value = "预览代码", crud = LogCrudEnum.R)
    @LogNoRet
    @RequestMapping(value = "/preview", method = RequestMethod.POST)
    @ResponseBody
    public Ret<CodeGenRetVo> preview(@RequestBody CodeGenReqVo codeGenReqVo) {
        CodeGenRetVo data = generatorBiz.preview(codeGenReqVo);
        return ok(data);
    }

    @FaLogOpr(value = "复制全部java文件", crud = LogCrudEnum.R)
    @LogNoRet
    @RequestMapping(value = "/copyJava", method = RequestMethod.POST)
    @ResponseBody
    public Ret<Boolean> copyJava(@RequestBody CodeCopyVo codeCopyVo) throws IOException {
        generatorBiz.copyJava(codeCopyVo);
        return ok();
    }

}
