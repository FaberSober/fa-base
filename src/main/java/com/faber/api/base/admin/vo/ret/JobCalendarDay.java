package com.faber.api.base.admin.vo.ret;

import lombok.Data;
import java.time.LocalDate;

@Data
public class JobCalendarDay {
    private LocalDate date;
    private long total;
    private long success;
    private long failed;
}
