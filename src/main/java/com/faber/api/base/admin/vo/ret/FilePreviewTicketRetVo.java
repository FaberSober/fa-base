package com.faber.api.base.admin.vo.ret;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文件预览 ticket 返回值。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FilePreviewTicketRetVo {

    private String ticket;
    private long expiresAt;
}
