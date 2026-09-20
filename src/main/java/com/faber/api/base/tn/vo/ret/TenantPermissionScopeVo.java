package com.faber.api.base.tn.vo.ret;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** 租户权限范围：平台必选 M、租户可选 O_t 和最终有效 C_t。 */
@Data
public class TenantPermissionScopeVo implements Serializable {

    private List<Long> requiredMenuIds = new ArrayList<>();

    private List<Long> optionalMenuIds = new ArrayList<>();

    private List<Long> menuIds = new ArrayList<>();
}
