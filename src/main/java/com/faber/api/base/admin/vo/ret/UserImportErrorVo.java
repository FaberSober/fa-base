package com.faber.api.base.admin.vo.ret;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 用户导入单行校验错误。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserImportErrorVo {

    private int rowNumber;
    private String username;
    private String message;
}
