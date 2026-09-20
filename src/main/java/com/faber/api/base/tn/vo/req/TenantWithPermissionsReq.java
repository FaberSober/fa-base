package com.faber.api.base.tn.vo.req;

import com.faber.api.base.tn.entity.Tenant;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 租户及权限范围请求。
 */
@Data
public class TenantWithPermissionsReq implements Serializable {

    @Valid
    @NotNull
    private Tenant tenant;

    @NotNull
    /** 新接口语义为 O_t；旧客户端仍可提交 menuIds。 */
    @JsonProperty("optionalMenuIds")
    @JsonAlias("menuIds")
    private List<Long> menuIds = new ArrayList<>();
}
