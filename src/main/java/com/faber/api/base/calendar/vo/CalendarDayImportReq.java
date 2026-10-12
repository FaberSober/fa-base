package com.faber.api.base.calendar.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 年度日历差异预览/发布请求。 */
@Data
public class CalendarDayImportReq {

    @NotBlank
    @Size(max = 32)
    private String calendarCode;

    @Size(max = 64)
    private String source;

    @Size(max = 64)
    private String sourceVersion;

    @NotEmpty
    @Size(max = 5000)
    private List<@Valid CalendarDayImportItem> days = new ArrayList<>();
}
