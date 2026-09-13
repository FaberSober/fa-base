package com.faber.api.base.calendar.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 外部年度日历预览请求。 */
@Data
public class CalendarExternalImportYearReq {

    @NotNull
    @Min(1970)
    @Max(2200)
    private Integer year;
}
