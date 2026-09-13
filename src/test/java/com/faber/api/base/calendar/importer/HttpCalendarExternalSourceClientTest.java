package com.faber.api.base.calendar.importer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpCalendarExternalSourceClientTest {

    @Test
    void parsesHolidayCnAndAkToolsPayloads() throws Exception {
        CalendarImportProperties properties = new CalendarImportProperties();
        properties.setHolidayUrlTemplate("https://holiday.example/{year}.json");
        properties.setAkToolsUrl("https://aktools.example:8080");

        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> holidayResponse = response("""
                {
                  "year": 2026,
                  "papers": [],
                  "days": [
                    {"name": "元旦", "date": "2026-01-01", "isOffDay": true},
                    {"name": "元旦调休", "date": "2026-01-04", "isOffDay": false}
                  ]
                }
                """);
        HttpResponse<String> tradingResponse = response("""
                {
                  "columns": ["trade_date"],
                  "index": [0, 1, 2],
                  "data": [["2026-01-02"], ["2026-01-10"], ["2025-12-31"]]
                }
                """);
        doReturn(holidayResponse, tradingResponse).when(httpClient).send(any(), any());

        CalendarExternalSourceData result = new HttpCalendarExternalSourceClient(
                new ObjectMapper(), properties, httpClient).fetch(2026);

        assertEquals("元旦", result.holidayDays().get(LocalDate.of(2026, 1, 1)).name());
        assertEquals(Set.of(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 10)), result.aShareTradingDates());
        assertEquals("HOLIDAY_CN", result.holidaySource());
        assertEquals("AKTOOLS_AKSHARE", result.aShareSource());
        assertEquals("2026-01-10", result.aShareSourceVersion());
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(String body) {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(body);
        return response;
    }
}
