package com.faber.api.base.admin.vo.ret;

import com.faber.core.license.LicenseMode;
import com.faber.core.license.LicenseState;
import lombok.Data;

import java.time.Instant;

@Data
public class LicenseStatusVo {

    private boolean enabled;
    private LicenseMode mode;
    private LicenseState status;
    private String machineId;
    private String licenseId;
    private String product;
    private String customer;
    private Instant issuedAt;
    private Instant expireAt;
}
