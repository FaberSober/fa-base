package com.faber.api.base.admin.vo.query;

import lombok.Data;

@Data
public class OnlineUserQueryVo {
    private String keyword;
    private String source;
    private Boolean active;
}
