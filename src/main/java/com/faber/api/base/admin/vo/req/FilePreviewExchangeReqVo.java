package com.faber.api.base.admin.vo.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 兑换文件预览 session 请求。
 */
@Data
public class FilePreviewExchangeReqVo {

    @NotBlank
    private String ticket;
}
