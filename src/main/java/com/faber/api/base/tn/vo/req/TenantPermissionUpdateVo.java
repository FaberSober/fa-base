package com.faber.api.base.tn.vo.req;

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
    private List<Long> menuIds = new ArrayList<>();

}
