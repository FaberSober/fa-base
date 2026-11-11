package com.faber.api.base.admin.mapper;

import com.faber.core.config.mybatis.base.FaBaseMapper;
import com.faber.api.base.admin.entity.JobLog;
import org.apache.ibatis.annotations.Param;

/**
 * BASE-系统定时任务-执行日志
 * 
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-07 16:03:51
 */
public interface JobLogMapper extends FaBaseMapper<JobLog> {
    java.util.List<com.faber.api.base.admin.vo.ret.JobCalendarDay> calendar(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end);
    java.util.List<com.faber.api.base.admin.vo.ret.JobTimelineEntry> timeline(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end,
            @Param("type") Integer type, @Param("status") Integer status, @Param("keyword") String keyword);
    com.faber.api.base.admin.vo.ret.JobTimelineStats timelineStats(@Param("start") java.time.LocalDateTime start, @Param("end") java.time.LocalDateTime end,
            @Param("type") Integer type, @Param("status") Integer status, @Param("keyword") String keyword);
    int publishDispatch(@Param("record") JobLog record);
    int finishExecution(@Param("record") JobLog record);
}
