package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/** 用户导入提交结果。 */
@Data
public class UserImportResultVo {

    private int totalCount;
    private int createCount;
    private int updateCount;
}
