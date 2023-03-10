package com.faber.api.base.doc.rest;

import com.faber.api.base.doc.biz.OnlyofficeBiz;
import com.faber.api.base.doc.dto.Track;
import com.faber.api.base.doc.models.filemodel.FileModel;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.utils.BaseResHandler;
import com.faber.core.vo.msg.Ret;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * Onlyoffice在线文档对接
 * @author Farando
 * @date 2023/3/13 16:32
 * @description
 */
@RestController
@RequestMapping("/api/base/doc/onlyoffice")
public class OnlyofficeController extends BaseResHandler {

    @Resource
    OnlyofficeBiz onlyofficeBiz;

    @FaLogOpr("打开文件Token")
    @GetMapping("/getFile/{fileId}")
    @ResponseBody
    public Ret<FileModel> openFile(@PathVariable("fileId") String fileId) {
        FileModel data = onlyofficeBiz.openFile(fileId);
        return ok(data);
    }

    @FaLogOpr("onlyoffice回调")
    @GetMapping("/track")
    @ResponseBody
    @IgnoreUserToken
    public Ret<Boolean> track(@RequestBody final Track body) {
        onlyofficeBiz.track(body);
        return ok();
    }

}
