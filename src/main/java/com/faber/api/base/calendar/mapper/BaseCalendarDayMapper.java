package com.faber.api.base.calendar.mapper;

import com.faber.api.base.calendar.entity.BaseCalendarDay;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/** 统一日历日期数据访问。 */
public interface BaseCalendarDayMapper extends FaBaseMapper<BaseCalendarDay> {

    BaseCalendarDay selectActiveByCalendarCodeAndDate(@Param("calendarCode") String calendarCode,
                                                       @Param("calendarDate") LocalDate calendarDate,
                                                       @Param("deleted") Boolean deleted);
}
