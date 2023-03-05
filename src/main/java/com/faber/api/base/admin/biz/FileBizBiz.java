package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.FileSave;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.faber.api.base.admin.entity.FileBiz;
import com.faber.api.base.admin.mapper.FileBizMapper;
import com.faber.core.web.biz.BaseBiz;

import javax.annotation.Resource;

/**
 * BASE-通用业务附件表
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2023-03-05 14:49:12
 */
@Service
public class FileBizBiz extends BaseBiz<FileBizMapper,FileBiz> {

    @Lazy
    @Resource
    FileSaveBiz fileSaveBiz;

    @Override
    protected void saveBefore(FileBiz entity) {
        FileSave fileSave = fileSaveBiz.getById(entity.getFileId());
        entity.setFileName(fileSave.getOriginalFilename());
        entity.setExt(fileSave.getExt());

        super.saveBefore(entity);
    }

}