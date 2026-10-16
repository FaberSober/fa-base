package com.faber.api.portal.file.rest;

import com.faber.api.base.admin.biz.FilePreviewTicketBiz;
import com.faber.api.base.admin.biz.FileSaveBiz;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.base.admin.vo.req.FilePreviewTicketReqVo;
import com.faber.api.base.admin.vo.ret.FilePreviewTicketRetVo;
import com.faber.api.base.telemetry.annotation.StatEvent;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@FaLogBiz("Portal-文件")
@RestController
@RequestMapping("/api/portal/fileSave")
public class PortalFileSaveController extends BaseResHandler {

    @Resource
    private FileSaveBiz fileSaveBiz;

    @Resource
    private FilePreviewTicketBiz filePreviewTicketBiz;

    @FaLogOpr(value = "上传文件", crud = LogCrudEnum.C)
    @StatEvent(value = "file.upload", module = "file", bizType = "file")
    @PostMapping("/upload")
    public Ret<FileSave> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return ok(fileSaveBiz.upload(file));
    }

    @FaLogOpr("创建文件预览凭证")
    @LogNoRet
    @PostMapping("/createPreviewTicket")
    public Ret<FilePreviewTicketRetVo> createPreviewTicket(@Valid @RequestBody FilePreviewTicketReqVo request) {
        return ok(filePreviewTicketBiz.createTicket(request.getFileId()));
    }
}
