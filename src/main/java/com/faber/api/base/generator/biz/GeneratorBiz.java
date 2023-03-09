package com.faber.api.base.generator.biz;

import com.faber.api.base.generator.mapper.GeneratorMapper;
import com.faber.api.base.generator.vo.req.TableQueryVo;
import com.faber.api.base.generator.vo.ret.TableVo;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.BasePageQuery;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

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
                .doSelectPageInfo(() -> generatorMapper.queryTable(query.getQuery()));
        return new TableRet<>(info);
    }
}
