package com.faber.api.base.telemetry.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelemetryDateFormatTest {

    @Test
    void acceptsSdkUtcAndOffsetDatesDespiteGlobalDateFormat() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        SimpleDateFormat globalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        globalFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        mapper.setDateFormat(globalFormat);

        Instant expected = Instant.parse("2026-09-05T06:44:55.511Z");
        for (Class<? extends TelemetryBaseReq> type : List.of(TelemetryEventReq.class, TelemetryErrorReq.class)) {
            for (String value : new String[]{"2026-09-05T06:44:55.511Z", "2026-09-05T14:44:55.511+08:00"}) {
                TelemetryBaseReq request = mapper.readValue("{\"occurTime\":\"" + value + "\"}", type);
                assertEquals(expected, request.getOccurTime().toInstant());
            }
        }
    }
}
