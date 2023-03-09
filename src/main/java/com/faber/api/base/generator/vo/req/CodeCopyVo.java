package com.faber.api.base.generator.vo.req;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author Farando
 * @date 2023/3/9 14:47
 * @description
 */
@Data
public class CodeCopyVo implements Serializable {

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
     * 生成的表名s
     */
    private List<String> tableNames;

    /**
     * 作者
     */
    private String author;

    /**
     * 作者email
     */
    private String email;

    /**
     * Java复制项目
     */
    private String javaCopyPath;

}
