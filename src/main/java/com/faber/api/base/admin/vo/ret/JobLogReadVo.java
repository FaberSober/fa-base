package com.faber.api.base.admin.vo.ret;

import com.faber.api.base.admin.enums.JobLogStatusEnum;
import lombok.Data;

/** 单次执行的增量日志；游标使用字节位置，不由前端按字符计算。 */
@Data
public class JobLogReadVo {
    private String content = "";
    private String nextOffset = "0";
    private JobLogStatusEnum status;
    private boolean hasMore;
    private boolean available;
    private String message;
}
