package com.faber.api.base.admin.biz;

import cn.hutool.core.util.StrUtil;
import com.faber.api.base.admin.entity.FileSave;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.faber.api.base.admin.entity.FileBiz;
import com.faber.api.base.admin.mapper.FileBizMapper;
import com.faber.core.web.biz.BaseBiz;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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

    public FileBiz saveFile(String mainBizId, String bizId, String type, String fileId) {
        FileBiz fileBiz = new FileBiz();
        fileBiz.setMainBizId(mainBizId);
        fileBiz.setBizId(bizId);
        fileBiz.setType(type);
        fileBiz.setFileId(fileId);
        this.save(fileBiz);
        return fileBiz;
    }

    /**
     * 批量保存业务数据
     * @param mainBizId 主业务ID，如：订单ID
     * @param bizId 业务ID，如：订单明细ID，可以为空
     * @param type 业务类型，如：订单附件 ORDER_FILES
     * @param fileIds 附件ID列表
     * @return
     */
    public List<FileBiz> saveBatch(Object mainBizId, Object bizId, String type, List<String> fileIds) {
        // delete before save
        this.lambdaUpdate()
                .eq(FileBiz::getMainBizId, mainBizId)
                .eq(FileBiz::getBizId, bizId)
                .eq(FileBiz::getType, type)
                .remove();

        List<FileBiz> list = new ArrayList<>();
        for (String fileId : fileIds) {
            FileBiz fileBiz = this.saveFile(StrUtil.toString(mainBizId), StrUtil.toString(bizId), type, fileId);
            list.add(fileBiz);
        }
        return list;
    }

    public List<FileBiz> getFiles(Object mainBizId, Object bizId, String type) {
        return this.lambdaQuery()
               .eq(FileBiz::getMainBizId, mainBizId)
               .eq(FileBiz::getBizId, bizId)
               .eq(FileBiz::getType, type)
                .orderByAsc(FileBiz::getId)
               .list();
    }

}