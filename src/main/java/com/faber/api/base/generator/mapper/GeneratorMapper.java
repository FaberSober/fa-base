package com.faber.api.base.generator.mapper;

import com.faber.api.base.generator.vo.req.TableQueryVo;
import com.faber.api.base.generator.vo.ret.TableVo;

import java.util.List;

/**
 * @author Farando
 * @date 2023/3/9 11:00
 * @description
 */
public interface GeneratorMapper {

    List<TableVo> queryTable(TableQueryVo tableQueryVo);

}
