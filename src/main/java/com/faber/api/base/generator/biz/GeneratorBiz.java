package com.faber.api.base.generator.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import com.faber.api.base.generator.enums.GeneratorTypeEnum;
import com.faber.api.base.generator.mapper.GeneratorMapper;
import com.faber.api.base.generator.utils.GeneratorUtils;
import com.faber.api.base.generator.vo.req.CodeCopyVo;
import com.faber.api.base.generator.vo.req.CodeGenReqVo;
import com.faber.api.base.generator.vo.req.TableQueryVo;
import com.faber.api.base.generator.vo.ret.CodeGenRetVo;
import com.faber.api.base.generator.vo.ret.ColumnVo;
import com.faber.api.base.generator.vo.ret.TableVo;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaFileUtils;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author Farando
 * @date 2023/3/9 11:00
 * @description
 */
@Service
public class GeneratorBiz {

    @Resource
    GeneratorMapper generatorMapper;

    public TableRet<TableVo> pageTable(BasePageQuery<TableQueryVo> query) {
        PageInfo<TableVo> info = PageHelper.startPage(query.getCurrent(), query.getPageSize())
                .doSelectPageInfo(() -> generatorMapper.queryTable(query.getQuery(), query.getSorter()));
        return new TableRet<>(info);
    }

    public CodeGenRetVo preview(CodeGenReqVo codeGenReqVo) {
        CodeGenRetVo retVo = new CodeGenRetVo();
        BeanUtil.copyProperties(codeGenReqVo, retVo);

        List<ColumnVo> columnVoList = generatorMapper.queryColumns(codeGenReqVo.getTableName());

        TableVo tableVo = generatorMapper.getTableByName(codeGenReqVo.getTableName());
        String code = GeneratorUtils.generatorCode(codeGenReqVo, tableVo, columnVoList);
        retVo.setCode(code);

        return retVo;
    }

    public void copyJava(CodeCopyVo codeCopyVo) throws IOException {
        if (codeCopyVo.getTableNames() == null || codeCopyVo.getTableNames().isEmpty()) {
            throw new BuzzException("请选择表");
        }

        for (String tableName: codeCopyVo.getTableNames()) {
            CodeGenReqVo codeGenReqVo = new CodeGenReqVo();
            BeanUtil.copyProperties(codeCopyVo, codeGenReqVo);
            codeGenReqVo.setTableName(tableName);

            List<ColumnVo> columnVoList = generatorMapper.queryColumns(codeGenReqVo.getTableName());
            TableVo tableVo = generatorMapper.getTableByName(codeGenReqVo.getTableName());

            codeGenReqVo.setType(GeneratorTypeEnum.JAVA_ENTITY);
            String code = GeneratorUtils.generatorCode(codeGenReqVo, tableVo, columnVoList);
            String path = GeneratorUtils.getJavaCopyPath(codeGenReqVo);

            FileUtil.writeString(path, code, StandardCharsets.UTF_8);
        }
    }

}
