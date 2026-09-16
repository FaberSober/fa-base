package com.faber.api.base.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 创建文件预览 ticket 请求。
 */
@Data
public class FilePreviewTicketReqVo {

    @NotBlank
    private String fileId;
}
