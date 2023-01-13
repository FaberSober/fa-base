package com.faber.api.base.admin.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.URLUtil;
import cn.xuyanwu.spring.file.storage.FileInfo;
import cn.xuyanwu.spring.file.storage.FileStorageService;
import cn.xuyanwu.spring.file.storage.UploadPretreatment;
import cn.xuyanwu.spring.file.storage.platform.LocalPlusFileStorage;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.base.admin.mapper.FileSaveMapper;
import com.faber.core.utils.FaFileUtils;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2019-08-19 10:09:36
 */
@Service
public class FileSaveBiz extends BaseBiz<FileSaveMapper, FileSave> {

    @Autowired
    private FileStorageService fileStorageService;

    public FileSave upload(MultipartFile file) {
        UploadPretreatment uploadPretreatment = fileStorageService.of(file);

        String extName = FileNameUtil.extName(file.getOriginalFilename());
        if (FaFileUtils.isImg(extName)) {
            uploadPretreatment = uploadPretreatment.thumbnail(200, 200);
        }

        FileInfo fileInfo = uploadPretreatment
                .setPath(DateUtil.today() + "/")
                .setSaveFilename(FaFileUtils.addTimestampToFileName(file.getOriginalFilename()))
                .upload();

        FileSave fileSave = new FileSave();
        BeanUtil.copyProperties(fileInfo, fileSave);

//        ((LocalPlusFileStorage)fileStorageService.getFileStorage("local-plus-1")).setStoragePath();

        super.save(fileSave);
        return fileSave;
    }

    public void getFile(String fileId) throws IOException {
        FileSave fileSave = getById(fileId);

        // 本地存储
        if (fileSave.getPlatform().startsWith("local-")) {
            LocalPlusFileStorage storage = ((LocalPlusFileStorage)fileStorageService.getFileStorage("local-plus-1"));
            String fileFullPath = storage.getAbsolutePath(fileSave.getUrl());

            FaFileUtils.downloadFile(new File(fileFullPath), fileSave.getOriginalFilename());
        } else {
            // TO-DO 其他下载渠道直接返回URL
            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
            response.sendRedirect(URLUtil.encode(fileSave.getUrl()));
        }
    }

    public void getFilePreview(String fileId) throws IOException {
        FileSave fileSave = getById(fileId);

        // 本地存储
        if (fileSave.getPlatform().startsWith("local-")) {
            LocalPlusFileStorage storage = ((LocalPlusFileStorage)fileStorageService.getFileStorage("local-plus-1"));
            String fileFullPath = storage.getAbsolutePath(fileSave.getThUrl());

            FaFileUtils.downloadFile(new File(fileFullPath), fileSave.getOriginalFilename());
        } else {
            // TO-DO 其他下载渠道直接返回URL
            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
            response.sendRedirect(URLUtil.encode(fileSave.getThUrl()));
        }
    }

}
