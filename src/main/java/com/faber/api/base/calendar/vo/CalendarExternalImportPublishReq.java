package com.faber.api.base.calendar.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 外部年度日历确认发布请求。 */
@Data
public class CalendarExternalImportPublishReq {

    @NotNull
    @Min(1970)
    @Max(2200)
    private Integer year;

    @NotEmpty
    @Size(max = 2)
    private List<@Valid CalendarDayImportReq> imports = new ArrayList<>();
}
