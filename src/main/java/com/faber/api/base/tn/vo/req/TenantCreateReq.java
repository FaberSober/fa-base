package com.faber.api.base.tn.vo.req;

import com.faber.api.base.tn.entity.Tenant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 租户及初始权限范围创建请求。
 */
@Data
public class TenantCreateReq implements Serializable {

    @Valid
    @NotNull
    private Tenant tenant;

    @NotNull
    private List<Long> menuIds = new ArrayList<>();
}
