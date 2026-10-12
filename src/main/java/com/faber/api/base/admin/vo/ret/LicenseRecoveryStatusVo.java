package com.faber.api.base.admin.vo.ret;

import com.faber.core.license.LicenseMode;
import com.faber.core.license.LicenseState;
import lombok.Data;

@Data
public class LicenseRecoveryStatusVo {

    private boolean matched;
    private boolean uploadAllowed;
    private LicenseMode mode;
    private LicenseState status;
}
