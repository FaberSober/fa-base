package com.faber.api.base.tn.vo.req;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class TenantPermissionUpdateVo implements Serializable {

    @NotBlank
    private String tenantId;

    @NotNull
    /** 新接口语义为 O_t；旧客户端仍可提交 menuIds。 */
    @JsonProperty("optionalMenuIds")
    @JsonAlias("menuIds")
    private List<Long> menuIds = new ArrayList<>();

}
