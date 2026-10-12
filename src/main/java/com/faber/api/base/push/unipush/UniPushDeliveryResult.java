package com.faber.api.base.push.unipush;

/** Result returned by the UniPush provider for one CID. */
public record UniPushDeliveryResult(
        String status,
        String providerStatus,
        String providerTaskId,
        String message,
        boolean invalidClientId) {
}
