package com.faber.api.base.tn.vo.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 保存当前用户的租户排序。 */
@Data
public class TenantPanelOrderReq {

    @NotEmpty
    private List<@NotBlank String> tenantIds;
}
