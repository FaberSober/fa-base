package com.faber.api.base.calendar.importer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faber.core.exception.BuzzException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.Year;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 从 holiday-cn 和 AKTools 获取年度日历。
 *
 * <p>AKTools 暴露的是 AKShare 的 {@code tool_trade_date_hist_sina} 接口，后端只解析
 * trade_date 字段，不让浏览器直接访问外部数据源。</p>
 */
@Slf4j
@Service
public class HttpCalendarExternalSourceClient implements CalendarExternalSourceClient {

    private static final String HOLIDAY_SOURCE = "HOLIDAY_CN";
    private static final String A_SHARE_SOURCE = "AKTOOLS_AKSHARE";
    private static final String A_SHARE_ENDPOINT = "/api/public/tool_trade_date_hist_sina";
    private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final ObjectMapper objectMapper;
    private final CalendarImportProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public HttpCalendarExternalSourceClient(ObjectMapper objectMapper,
                                            CalendarImportProperties properties) {
        this(objectMapper, properties, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(requirePositive(properties.getConnectTimeoutSeconds(), "连接超时")))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    HttpCalendarExternalSourceClient(ObjectMapper objectMapper,
                                     CalendarImportProperties properties,
                                     HttpClient httpClient) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public CalendarExternalSourceData fetch(int year) {
        validateYear(year);

        String holidayBody = get(buildHolidayUri(year), "holiday-cn");
        JsonNode holidayRoot = readJson(holidayBody, "holiday-cn");
        Map<LocalDate, CalendarExternalSourceData.HolidayDay> holidayDays = parseHolidayDays(holidayRoot, year);

        String akToolsUrl = properties.getAkToolsUrl();
        if (akToolsUrl == null || akToolsUrl.isBlank()) {
            throw new BuzzException("未配置 AKTools 地址，请设置 fa.calendar.import.ak-tools-url");
        }
        String tradingBody = get(buildAkToolsUri(akToolsUrl), "AKTools A股交易日历");
        JsonNode tradingRoot = readJson(tradingBody, "AKTools A股交易日历");
        Set<LocalDate> tradingDates = parseTradingDates(tradingRoot, year);

        for (CalendarExternalSourceData.HolidayDay holidayDay : holidayDays.values()) {
            if (holidayDay.offDay() && tradingDates.contains(holidayDay.date())) {
                throw new BuzzException("节假日源与A股交易日源冲突: " + holidayDay.date());
            }
        }

        return new CalendarExternalSourceData(
                year,
                holidayDays,
                tradingDates,
                HOLIDAY_SOURCE,
                String.valueOf(year),
                A_SHARE_SOURCE,
                tradingDates.stream().max(LocalDate::compareTo).map(LocalDate::toString).orElse(String.valueOf(year)));
    }

    private Map<LocalDate, CalendarExternalSourceData.HolidayDay> parseHolidayDays(JsonNode root, int year) {
        if (!root.path("year").canConvertToInt() || root.path("year").asInt() != year) {
            throw new BuzzException("holiday-cn 返回年份不匹配: " + year);
        }
        JsonNode days = root.get("days");
        if (days == null || !days.isArray()) {
            throw new BuzzException("holiday-cn 返回缺少 days 数组");
        }

        Map<LocalDate, CalendarExternalSourceData.HolidayDay> result = new HashMap<>();
        for (JsonNode day : days) {
            String dateText = requiredText(day, "date", "holiday-cn");
            LocalDate date = parseDate(dateText, "holiday-cn");
            if (date.getYear() != year) {
                throw new BuzzException("holiday-cn 包含目标年份外的日期: " + date);
            }
            String name = requiredText(day, "name", "holiday-cn");
            JsonNode offDay = day.get("isOffDay");
            if (offDay == null || !offDay.isBoolean()) {
                throw new BuzzException("holiday-cn 日期缺少 isOffDay: " + date);
            }
            CalendarExternalSourceData.HolidayDay old = result.put(
                    date,
                    new CalendarExternalSourceData.HolidayDay(date, name, offDay.asBoolean()));
            if (old != null) {
                throw new BuzzException("holiday-cn 存在重复日期: " + date);
            }
        }
        return result;
    }

    private Set<LocalDate> parseTradingDates(JsonNode root, int year) {
        JsonNode rows = locateRows(root);
        if (rows == null || !rows.isArray()) {
            throw new BuzzException("AKTools 返回缺少交易日数组");
        }

        int tradeDateIndex = locateTradeDateColumn(root);
        Set<LocalDate> result = new HashSet<>();
        for (JsonNode row : rows) {
            String dateText = extractTradeDate(row, tradeDateIndex);
            if (dateText == null || dateText.isBlank()) {
                throw new BuzzException("AKTools 返回存在缺少 trade_date 的记录");
            }
            LocalDate date = parseDate(dateText, "AKTools A股交易日历");
            if (date.getYear() == year) {
                result.add(date);
            }
        }
        if (result.isEmpty()) {
            throw new BuzzException("AKTools 返回的交易日数据不包含年份: " + year);
        }
        return result;
    }

    private JsonNode locateRows(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (!root.isObject()) {
            return null;
        }
        for (String field : new String[]{"data", "rows", "result"}) {
            JsonNode child = root.get(field);
            if (child == null) {
                continue;
            }
            if (child.isArray()) {
                return child;
            }
            JsonNode nested = locateRows(child);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private int locateTradeDateColumn(JsonNode root) {
        JsonNode columns = root == null ? null : root.get("columns");
        if (columns == null || !columns.isArray()) {
            return -1;
        }
        for (int i = 0; i < columns.size(); i++) {
            if ("trade_date".equals(columns.get(i).asText()) || "tradeDate".equals(columns.get(i).asText())) {
                return i;
            }
        }
        return -1;
    }

    private String extractTradeDate(JsonNode row, int tradeDateIndex) {
        if (row.isTextual()) {
            return row.asText();
        }
        if (row.isObject()) {
            for (String field : new String[]{"trade_date", "tradeDate", "date"}) {
                JsonNode value = row.get(field);
                if (value != null && !value.isNull()) {
                    return value.asText();
                }
            }
            return null;
        }
        if (row.isArray()) {
            int index = tradeDateIndex >= 0 ? tradeDateIndex : row.size() == 1 ? 0 : -1;
            if (index >= 0 && index < row.size() && !row.get(index).isNull()) {
                return row.get(index).asText();
            }
        }
        return null;
    }

    private String requiredText(JsonNode node, String field, String source) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull() || value.asText().isBlank()) {
            throw new BuzzException(source + " 日期缺少字段: " + field);
        }
        return value.asText().trim();
    }

    private LocalDate parseDate(String value, String source) {
        String normalized = value.trim();
        if (normalized.length() >= 10 && (normalized.charAt(4) == '-' || normalized.charAt(4) == '/')) {
            normalized = normalized.substring(0, 10).replace('/', '-');
        }
        try {
            if (normalized.matches("\\d{8}")) {
                return LocalDate.parse(normalized, BASIC_DATE);
            }
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException e) {
            throw new BuzzException(source + " 返回非法日期: " + value);
        }
    }

    private URI buildHolidayUri(int year) {
        String template = properties.getHolidayUrlTemplate();
        if (template == null || template.isBlank()) {
            throw new BuzzException("holiday-cn 地址不能为空");
        }
        return requireHttpUri(template.replace("{year}", String.valueOf(year)), "holiday-cn");
    }

    private URI buildAkToolsUri(String baseUrl) {
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        String endpoint = base.endsWith("/api/public")
                ? base + "/tool_trade_date_hist_sina"
                : base + A_SHARE_ENDPOINT;
        return requireHttpUri(endpoint, "AKTools");
    }

    private URI requireHttpUri(String value, String source) {
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if ((scheme == null || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null) {
                throw new IllegalArgumentException("unsupported URI");
            }
            return uri;
        } catch (IllegalArgumentException e) {
            throw new BuzzException(source + " 地址不合法");
        }
    }

    private String get(URI uri, String source) {
        try {
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(requirePositive(properties.getRequestTimeoutSeconds(), "请求超时")))
                    .header("Accept", "application/json,text/plain")
                    .header("User-Agent", "fa-admin-calendar/1.0")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BuzzException(source + " 返回状态码: " + response.statusCode());
            }
            String body = response.body();
            if (body == null || body.isBlank()) {
                throw new BuzzException(source + " 返回空内容");
            }
            if (body.getBytes(StandardCharsets.UTF_8).length > properties.getMaxResponseBytes()) {
                throw new BuzzException(source + " 返回内容超过大小限制");
            }
            return body;
        } catch (BuzzException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BuzzException(source + " 请求被中断");
        } catch (IOException | IllegalArgumentException e) {
            log.warn("calendar external source request failed. source={}, uri={}", source, uri, e);
            throw new BuzzException(source + " 请求失败: " + e.getMessage());
        }
    }

    private JsonNode readJson(String body, String source) {
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root == null || root.isNull()) {
                throw new BuzzException(source + " 返回空 JSON");
            }
            return root;
        } catch (BuzzException e) {
            throw e;
        } catch (JsonProcessingException e) {
            throw new BuzzException(source + " 返回 JSON 格式不正确");
        }
    }

    private static int requirePositive(int value, String label) {
        if (value <= 0) {
            throw new BuzzException(label + "必须大于 0");
        }
        return value;
    }

    private void validateYear(int year) {
        if (year < 1970 || year > 2200) {
            throw new BuzzException("年份范围必须为 1970-2200");
        }
    }
}
