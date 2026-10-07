package com.faber.api.base.push.vo.ret;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BrowserPushSubscriptionStatusRetVo {
    private boolean subscribed;
}
