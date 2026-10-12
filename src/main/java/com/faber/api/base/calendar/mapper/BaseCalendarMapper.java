package com.faber.api.base.calendar.mapper;

import com.faber.api.base.calendar.entity.BaseCalendar;
import com.faber.core.config.mybatis.base.FaBaseMapper;
import org.apache.ibatis.annotations.Param;

/** 统一日历定义数据访问。 */
public interface BaseCalendarMapper extends FaBaseMapper<BaseCalendar> {

    BaseCalendar selectEnabledByCode(@Param("calendarCode") String calendarCode,
                                     @Param("enabled") Boolean enabled,
                                     @Param("deleted") Boolean deleted);
}
