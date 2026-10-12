package com.faber.api.base.calendar.importer;

import com.faber.api.base.calendar.biz.BaseCalendarDayBiz;
import com.faber.api.base.calendar.enums.BaseCalendarDayTypeEnum;
import com.faber.api.base.calendar.service.CalendarService;
import com.faber.api.base.calendar.vo.CalendarDayImportItem;
import com.faber.api.base.calendar.vo.CalendarDayImportPreviewVo;
import com.faber.api.base.calendar.vo.CalendarDayImportReq;
import com.faber.api.base.calendar.vo.CalendarExternalImportCalendarPreviewVo;
import com.faber.api.base.calendar.vo.CalendarExternalImportPreviewVo;
import com.faber.api.base.calendar.vo.CalendarExternalImportPublishReq;
import com.faber.api.base.calendar.vo.CalendarExternalImportYearReq;
import com.faber.core.exception.BuzzException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** 外部年度日历生成、差异预览和确认发布。 */
@Service
public class CalendarExternalImportService {

    private static final Set<String> SUPPORTED_CALENDARS = Set.of(
            CalendarService.CN_OA,
            CalendarService.CN_A_SHARE);

    private final CalendarExternalSourceClient sourceClient;
    private final BaseCalendarDayBiz calendarDayBiz;

    public CalendarExternalImportService(CalendarExternalSourceClient sourceClient,
                                         BaseCalendarDayBiz calendarDayBiz) {
        this.sourceClient = sourceClient;
        this.calendarDayBiz = calendarDayBiz;
    }

    public CalendarExternalImportPreviewVo preview(CalendarExternalImportYearReq request) {
        if (request == null || request.getYear() == null) {
            throw new BuzzException("年份不能为空");
        }
        return preview(request.getYear());
    }

    public CalendarExternalImportPreviewVo preview(int year) {
        validateYear(year);
        CalendarExternalSourceData sourceData = sourceClient.fetch(year);
        if (sourceData.year() != year) {
            throw new BuzzException("外部日历数据年份不匹配: " + year);
        }
        List<CalendarDayImportReq> imports = buildImports(sourceData);
        List<CalendarExternalImportCalendarPreviewVo> calendars = new ArrayList<>();
        for (CalendarDayImportReq request : imports) {
            CalendarDayImportPreviewVo preview = calendarDayBiz.preview(request);
            calendars.add(new CalendarExternalImportCalendarPreviewVo(
                    request.getCalendarCode(),
                    request.getSource(),
                    request.getSourceVersion(),
                    preview));
        }
        return new CalendarExternalImportPreviewVo(year, calendars, imports);
    }

    @Transactional(rollbackFor = Exception.class)
    public CalendarExternalImportPreviewVo publish(CalendarExternalImportPublishReq request) {
        validatePublishRequest(request);
        List<CalendarExternalImportCalendarPreviewVo> calendars = new ArrayList<>();
        for (CalendarDayImportReq importRequest : request.getImports()) {
            CalendarDayImportPreviewVo preview = calendarDayBiz.publish(importRequest);
            calendars.add(new CalendarExternalImportCalendarPreviewVo(
                    importRequest.getCalendarCode(),
                    importRequest.getSource(),
                    importRequest.getSourceVersion(),
                    preview));
        }
        return new CalendarExternalImportPreviewVo(request.getYear(), calendars, request.getImports());
    }

    private List<CalendarDayImportReq> buildImports(CalendarExternalSourceData sourceData) {
        List<CalendarDayImportReq> imports = new ArrayList<>();
        imports.add(buildOaImport(sourceData));
        imports.add(buildAShareImport(sourceData));
        return imports;
    }

    private CalendarDayImportReq buildOaImport(CalendarExternalSourceData sourceData) {
        List<CalendarDayImportItem> days = new ArrayList<>();
        sourceData.holidayDays().values().stream()
                .sorted(Comparator.comparing(CalendarExternalSourceData.HolidayDay::date))
                .forEach(holidayDay -> days.add(item(holidayDay.date(),
                        holidayDay.offDay() ? BaseCalendarDayTypeEnum.HOLIDAY : BaseCalendarDayTypeEnum.MAKEUP_WORKDAY,
                        !holidayDay.offDay(),
                        holidayDay.name())));
        if (days.isEmpty()) {
            throw new BuzzException("holiday-cn 未返回年度例外日期: " + sourceData.year());
        }
        return request(CalendarService.CN_OA, sourceData.holidaySource(), sourceData.holidaySourceVersion(), days);
    }

    private CalendarDayImportReq buildAShareImport(CalendarExternalSourceData sourceData) {
        List<CalendarDayImportItem> days = new ArrayList<>();
        LocalDate lastKnownTradingDate = sourceData.aShareTradingDates().stream()
                .max(LocalDate::compareTo)
                .orElseThrow(() -> new BuzzException("AKTools 未返回年度交易日: " + sourceData.year()));
        for (LocalDate date : allDates(sourceData.year())) {
            CalendarExternalSourceData.HolidayDay holidayDay = sourceData.holidayDays().get(date);
            boolean isTradingDay = sourceData.aShareTradingDates().contains(date);
            if (isTradingDay && !isWeekday(date)) {
                days.add(item(date, BaseCalendarDayTypeEnum.SPECIAL_TRADING_DAY, true,
                        holidayDay == null ? null : holidayDay.name()));
            } else if (!isTradingDay && isWeekday(date) && !date.isAfter(lastKnownTradingDate)) {
                days.add(item(date, BaseCalendarDayTypeEnum.EXCHANGE_CLOSED, false,
                        holidayDay == null ? null : holidayDay.name()));
            }
        }
        if (days.isEmpty()) {
            throw new BuzzException("AKTools 未返回年度交易例外日期: " + sourceData.year());
        }
        return request(CalendarService.CN_A_SHARE, sourceData.aShareSource(), sourceData.aShareSourceVersion(), days);
    }

    private CalendarDayImportReq request(String calendarCode,
                                         String source,
                                         String sourceVersion,
                                         List<CalendarDayImportItem> days) {
        CalendarDayImportReq request = new CalendarDayImportReq();
        request.setCalendarCode(calendarCode);
        request.setSource(source);
        request.setSourceVersion(sourceVersion);
        request.setDays(days);
        return request;
    }

    private CalendarDayImportItem item(LocalDate date,
                                       BaseCalendarDayTypeEnum dayType,
                                       boolean isOpen,
                                       String holidayName) {
        CalendarDayImportItem item = new CalendarDayImportItem();
        item.setCalendarDate(date);
        item.setDayType(dayType);
        item.setIsOpen(isOpen);
        item.setHolidayName(holidayName);
        return item;
    }

    private void validatePublishRequest(CalendarExternalImportPublishReq request) {
        if (request == null || request.getYear() == null) {
            throw new BuzzException("年份不能为空");
        }
        int year = request.getYear();
        validateYear(year);
        if (request.getImports() == null || request.getImports().size() != SUPPORTED_CALENDARS.size()) {
            throw new BuzzException("外部年度发布必须同时包含 CN_OA 和 CN_A_SHARE");
        }

        Set<String> codes = new HashSet<>();
        for (CalendarDayImportReq importRequest : request.getImports()) {
            if (importRequest == null || importRequest.getCalendarCode() == null) {
                throw new BuzzException("外部年度发布缺少日历编码");
            }
            String code = importRequest.getCalendarCode().trim().toUpperCase(Locale.ROOT);
            if (!SUPPORTED_CALENDARS.contains(code) || !codes.add(code)) {
                throw new BuzzException("外部年度发布仅支持 CN_OA 和 CN_A_SHARE，且每套日历只能出现一次");
            }
            validateSparseImport(importRequest, year, code);
        }
    }

    private void validateSparseImport(CalendarDayImportReq request, int year, String calendarCode) {
        if (request.getDays() == null || request.getDays().isEmpty()) {
            throw new BuzzException(calendarCode + " 必须包含至少一条例外日期");
        }
        Set<BaseCalendarDayTypeEnum> allowedTypes = CalendarService.CN_OA.equals(calendarCode)
                ? Set.of(BaseCalendarDayTypeEnum.HOLIDAY, BaseCalendarDayTypeEnum.MAKEUP_WORKDAY)
                : Set.of(BaseCalendarDayTypeEnum.EXCHANGE_CLOSED, BaseCalendarDayTypeEnum.SPECIAL_TRADING_DAY);
        Set<LocalDate> dates = new HashSet<>();
        for (CalendarDayImportItem item : request.getDays()) {
            if (item == null || item.getCalendarDate() == null || !dates.add(item.getCalendarDate())
                    || item.getCalendarDate().getYear() != year) {
                throw new BuzzException(request.getCalendarCode() + " 存在重复或越界日期");
            }
            if (item.getDayType() == null || !allowedTypes.contains(item.getDayType())) {
                throw new BuzzException(calendarCode + " 只允许导入例外日期类型");
            }
        }
    }

    private List<LocalDate> allDates(int year) {
        int length = Year.of(year).length();
        LocalDate start = LocalDate.of(year, 1, 1);
        List<LocalDate> dates = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            dates.add(start.plusDays(i));
        }
        return dates;
    }

    private boolean isWeekday(LocalDate date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY;
    }

    private void validateYear(int year) {
        if (year < 1970 || year > 2200) {
            throw new BuzzException("年份范围必须为 1970-2200");
        }
    }
}
