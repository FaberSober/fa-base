package com.faber.api.base.tn.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.faber.core.annotation.SqlEquals;
import com.faber.core.bean.BaseDelEntity;
import com.faber.core.config.validator.validator.Vg;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * 租户可用权限范围。
 */
@Data
@TableName("tn_tenant_permission")
public class TenantPermission extends BaseDelEntity {

    @Null(groups = Vg.Crud.C.class)
    @NotNull(groups = Vg.Crud.U.class)
    @TableId(type = IdType.AUTO)
    private Long id;

    @NotBlank
    @SqlEquals
    private String tenantId;

    @NotNull
    @SqlEquals
    private Long menuId;

}
