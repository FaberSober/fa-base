package com.faber.api.base.push.vo.ret;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BrowserPushTestSendRetVo {
    private int targetCount;
    private int acceptedCount;
    private int failedCount;
}
