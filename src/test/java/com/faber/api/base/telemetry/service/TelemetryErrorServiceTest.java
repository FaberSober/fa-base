package com.faber.api.base.telemetry.service;

import com.faber.api.base.telemetry.entity.TelemetryApp;
import com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum;
import com.faber.api.base.telemetry.vo.TelemetryErrorReq;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelemetryErrorServiceTest {

    private final TelemetryErrorService telemetryErrorService = new TelemetryErrorService(null, null);

    @Test
    void shouldAggregateMessagesThatOnlyDifferByDynamicNumbers() {
        TelemetryApp app = new TelemetryApp();
        app.setId(1L);

        TelemetryErrorReq first = request("Cannot read item 100", "TypeError: Cannot read item 100\n  at render (app.tsx:20:3)");
        TelemetryErrorReq second = request("Cannot read item 200", "TypeError: Cannot read item 200\n  at render (app.tsx:30:3)");

        assertEquals(telemetryErrorService.fingerprint(app, first), telemetryErrorService.fingerprint(app, second));
    }

    private TelemetryErrorReq request(String message, String stack) {
        TelemetryErrorReq request = new TelemetryErrorReq();
        request.setClientType(TelemetryClientTypeEnum.WEB);
        request.setErrorType("TypeError");
        request.setMessage(message);
        request.setStack(stack);
        return request;
    }
}
