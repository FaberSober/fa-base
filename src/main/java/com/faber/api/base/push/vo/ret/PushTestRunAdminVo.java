package com.faber.api.base.push.vo.ret;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PushTestRunAdminVo {

    private String testId;
    private Long createdAt;
    private List<DeviceResult> devices = new ArrayList<>();

    @Data
    public static class DeviceResult {
        private Long deviceId;
        private String status;
        private String providerStatus;
        private String providerTaskId;
        private String message;
        private Long updatedAt;
    }
}
