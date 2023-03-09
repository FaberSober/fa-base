package com.faber.api.base.generator.vo.req;

import com.faber.api.base.generator.enums.GeneratorTypeEnum;
import lombok.Data;

import java.io.Serializable;

/**
 * @author Farando
 * @date 2023/3/9 14:47
 * @description
 */
@Data
public class CodeGenReqVo implements Serializable {

    /**
     * 包名
     */
    private String packageName;

    /**
     * 删除表名前缀
     */
    private String tablePrefix;

    /**
     * 前端模块前缀
     */
    private String mainModule;

    /**
     * 生成的表名
     */
    private String tableName;

    /**
     * 生成代码类型
     */
    private GeneratorTypeEnum type;

}
