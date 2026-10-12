package com.faber.api.base.telemetry.vo;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** Desktop 客户端写入 TelemetryBaseReq.context 的约定字段。 */
@Data
public class TelemetryDesktopContext {

    @Size(max = 64)
    private String platform;
    @Size(max = 128)
    private String osVersion;
    @Size(max = 64)
    private String arch;
    @Size(max = 128)
    private String appVersion;
    @Size(max = 128)
    private String tauriVersion;
    @Size(max = 32)
    private String runtime;
}
