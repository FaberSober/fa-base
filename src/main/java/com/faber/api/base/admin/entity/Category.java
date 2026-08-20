package com.faber.api.base.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.api.base.admin.enums.CategoryModuleEnum;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.annotation.SqlSorter;
import com.faber.core.annotation.SqlTreeId;
import com.faber.core.annotation.SqlTreeName;
import com.faber.core.annotation.SqlTreeParentId;
import com.faber.core.bean.BaseDelEntity;
import com.faber.core.config.validator.validator.Vg;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 通用业务分类树。
 */
@Data
@TableName("base_category")
public class Category extends BaseDelEntity {

    @Null(groups = Vg.Crud.C.class)
    @NotNull(groups = Vg.Crud.U.class)
    @TableId(type = IdType.AUTO)
    @SqlTreeId
    private Integer id;

    @NotNull
    @SqlEquals
    private CategoryModuleEnum module;

    @SqlTreeParentId
    private Integer parentId;

    @NotBlank
    @Size(max = 64)
    @SqlTreeName
    private String name;

    @Size(max = 256)
    private String description;

    @SqlSorter
    private Integer sort;
}
