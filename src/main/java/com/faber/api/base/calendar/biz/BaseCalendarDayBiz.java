package com.faber.api.base.calendar.biz;

import com.faber.api.base.calendar.entity.BaseCalendar;
import com.faber.api.base.calendar.entity.BaseCalendarDay;
import com.faber.api.base.calendar.enums.BaseCalendarDayTypeEnum;
import com.faber.api.base.calendar.mapper.BaseCalendarDayMapper;
import com.faber.api.base.calendar.mapper.BaseCalendarMapper;
import com.faber.api.base.calendar.vo.CalendarDayImportItem;
import com.faber.api.base.calendar.vo.CalendarDayImportPreviewVo;
import com.faber.api.base.calendar.vo.CalendarDayImportReq;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** 统一日历单日事实维护、差异预览和幂等发布。 */
@Service
public class BaseCalendarDayBiz extends BaseBiz<BaseCalendarDayMapper, BaseCalendarDay> {

    private static final Boolean ENABLED = Boolean.TRUE;
    private static final Boolean NOT_DELETED = Boolean.FALSE;

    private final BaseCalendarMapper calendarMapper;

    public BaseCalendarDayBiz(BaseCalendarMapper calendarMapper) {
        this.calendarMapper = calendarMapper;
    }

    @Override
    protected void saveBefore(BaseCalendarDay entity) {
        normalizeAndValidate(entity);
    }

    /** 查询某个日历的年度日期事实。 */
    public List<BaseCalendarDay> listByYear(String calendarCode, int year) {
        String code = normalizeCode(calendarCode);
        validateYear(year);
        requireEnabledCalendar(code);
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = start.plusYears(1);
        return lambdaQuery()
                .eq(BaseCalendarDay::getCalendarCode, code)
                .ge(BaseCalendarDay::getCalendarDate, start)
                .lt(BaseCalendarDay::getCalendarDate, end)
                .orderByAsc(BaseCalendarDay::getCalendarDate)
                .list();
    }

    /** 按日历日期幂等保存，不因重复导入产生重复事实。 */
    @Transactional(rollbackFor = Exception.class)
    public List<BaseCalendarDay> upsertBatch(CalendarDayImportReq request) {
        preview(request);
        List<BaseCalendarDay> result = new ArrayList<>();
        for (CalendarDayImportItem item : request.getDays()) {
            BaseCalendarDay entity = toEntity(request, item);
            BaseCalendarDay old = lambdaQuery()
                    .eq(BaseCalendarDay::getCalendarCode, entity.getCalendarCode())
                    .eq(BaseCalendarDay::getCalendarDate, entity.getCalendarDate())
                    .one();
            if (old == null) {
                save(entity);
            } else {
                entity.setId(old.getId());
                updateById(entity);
            }
            result.add(entity);
        }
        return result;
    }

    /** 计算导入批次相对于当前数据库的新增、修改和未变化记录。 */
    public CalendarDayImportPreviewVo preview(CalendarDayImportReq request) {
        validateRequest(request);
        String code = normalizeCode(request.getCalendarCode());
        requireEnabledCalendar(code);

        List<BaseCalendarDay> incoming = new ArrayList<>();
        Set<LocalDate> dates = new HashSet<>();
        for (CalendarDayImportItem item : request.getDays()) {
            if (!dates.add(item.getCalendarDate())) {
                throw new BuzzException("导入批次存在重复日期: " + item.getCalendarDate());
            }
            incoming.add(toEntity(request, item));
        }

        Map<LocalDate, BaseCalendarDay> existing = new HashMap<>();
        if (!dates.isEmpty()) {
            List<BaseCalendarDay> current = lambdaQuery()
                    .eq(BaseCalendarDay::getCalendarCode, code)
                    .in(BaseCalendarDay::getCalendarDate, dates)
                    .list();
            for (BaseCalendarDay day : current) {
                existing.put(day.getCalendarDate(), day);
            }
        }

        List<BaseCalendarDay> addedDays = new ArrayList<>();
        List<BaseCalendarDay> changedDays = new ArrayList<>();
        int unchanged = 0;
        for (BaseCalendarDay day : incoming) {
            BaseCalendarDay old = existing.get(day.getCalendarDate());
            if (old == null) {
                addedDays.add(day);
            } else if (sameBusinessValue(old, day)) {
                unchanged++;
            } else {
                day.setId(old.getId());
                changedDays.add(day);
            }
        }
        return new CalendarDayImportPreviewVo(code, incoming.size(), addedDays.size(),
                changedDays.size(), unchanged, addedDays, changedDays);
    }

    /** 发布导入批次；发布前再次校验并在同一事务内 upsert。 */
    @Transactional(rollbackFor = Exception.class)
    public CalendarDayImportPreviewVo publish(CalendarDayImportReq request) {
        CalendarDayImportPreviewVo preview = preview(request);
        for (CalendarDayImportItem item : request.getDays()) {
            BaseCalendarDay entity = toEntity(request, item);
            BaseCalendarDay old = lambdaQuery()
                    .eq(BaseCalendarDay::getCalendarCode, entity.getCalendarCode())
                    .eq(BaseCalendarDay::getCalendarDate, entity.getCalendarDate())
                    .one();
            if (old == null) {
                save(entity);
            } else {
                entity.setId(old.getId());
                updateById(entity);
            }
        }
        return preview;
    }

    @Override
    public boolean removeById(Serializable id) {
        throw new BuzzException("日历日期不支持删除，请更新为关闭日期");
    }

    private BaseCalendarDay toEntity(CalendarDayImportReq request, CalendarDayImportItem item) {
        BaseCalendarDay entity = new BaseCalendarDay();
        entity.setCalendarCode(normalizeCode(request.getCalendarCode()));
        entity.setCalendarDate(item.getCalendarDate());
        entity.setDayType(item.getDayType());
        entity.setIsOpen(item.getIsOpen());
        entity.setHolidayName(trimToNull(item.getHolidayName()));
        entity.setSource(defaultValue(request.getSource(), "MANUAL"));
        entity.setSourceVersion(trimToNull(request.getSourceVersion()));
        entity.setRemark(trimToNull(item.getRemark()));
        return entity;
    }

    private void validateRequest(CalendarDayImportReq request) {
        if (request == null || request.getDays() == null || request.getDays().isEmpty()) {
            throw new BuzzException("导入日期不能为空");
        }
        if (request.getCalendarCode() == null || request.getCalendarCode().isBlank()) {
            throw new BuzzException("日历编码不能为空");
        }
        for (int i = 0; i < request.getDays().size(); i++) {
            CalendarDayImportItem item = request.getDays().get(i);
            if (item == null || item.getCalendarDate() == null) {
                throw new BuzzException("第 " + (i + 1) + " 条日期不能为空");
            }
            if (item.getDayType() == null || item.getIsOpen() == null) {
                throw new BuzzException("第 " + (i + 1) + " 条日期类型和开放状态不能为空");
            }
            validateDayType(item.getDayType(), item.getIsOpen());
        }
    }

    private void normalizeAndValidate(BaseCalendarDay entity) {
        if (entity == null) {
            throw new BuzzException("日历日期不能为空");
        }
        entity.setCalendarCode(normalizeCode(entity.getCalendarCode()));
        if (entity.getIsOpen() == null) {
            throw new BuzzException("是否开放不能为空");
        }
        if (entity.getDayType() == null) {
            throw new BuzzException("日期类型不能为空");
        }
        validateDayType(entity.getDayType(), entity.getIsOpen());
        requireEnabledCalendar(entity.getCalendarCode());
        if (entity.getSource() == null || entity.getSource().isBlank()) {
            entity.setSource("MANUAL");
        }
    }

    private void validateDayType(BaseCalendarDayTypeEnum dayType, Boolean isOpen) {
        boolean open = Boolean.TRUE.equals(isOpen);
        boolean shouldOpen = switch (dayType) {
            case WORKDAY, MAKEUP_WORKDAY, SPECIAL_TRADING_DAY -> true;
            case WEEKEND, HOLIDAY, EXCHANGE_CLOSED -> false;
        };
        if (open != shouldOpen) {
            throw new BuzzException("日期类型与开放状态不一致: " + dayType.getValue());
        }
    }

    private void requireEnabledCalendar(String code) {
        if (calendarMapper.selectEnabledByCode(code, ENABLED, NOT_DELETED) == null) {
            throw new BuzzException("日历不存在或未启用: " + code);
        }
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BuzzException("日历编码不能为空");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private void validateYear(int year) {
        if (year < 1970 || year > 2200) {
            throw new BuzzException("年份范围必须为 1970-2200");
        }
    }

    private boolean sameBusinessValue(BaseCalendarDay old, BaseCalendarDay incoming) {
        return Objects.equals(old.getDayType(), incoming.getDayType())
                && Objects.equals(old.getIsOpen(), incoming.getIsOpen())
                && Objects.equals(old.getHolidayName(), incoming.getHolidayName())
                && Objects.equals(old.getSource(), incoming.getSource())
                && Objects.equals(old.getSourceVersion(), incoming.getSourceVersion())
                && Objects.equals(old.getRemark(), incoming.getRemark());
    }

    private String defaultValue(String value, String fallback) {
        String result = trimToNull(value);
        return result == null ? fallback : result;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
