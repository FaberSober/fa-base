package com.faber.api.base.admin.vo.ret;

import lombok.Data;

/**
 * 文件预览 session 资源信息。
 */
@Data
public class FilePreviewResourceRetVo {

    private String fileId;
    private String filename;
    private String contentType;
    private String ext;
    private Long size;
    private String previewUrl;
    private String downloadUrl;
    private boolean downloadAllowed;
    private String watermarkText;
}
