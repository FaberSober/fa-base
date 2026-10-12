package com.faber.api.base.admin.vo.ret;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 用户导入预览摘要。 */
@Data
public class UserImportPreviewVo {

    private int totalCount;
    private int validCount;
    private int errorCount;
    private int createCount;
    private int updateCount;
    private List<UserImportErrorVo> errors = new ArrayList<>();
}
