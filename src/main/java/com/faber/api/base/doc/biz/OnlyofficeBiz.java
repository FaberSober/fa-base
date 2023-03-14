package com.faber.api.base.doc.biz;

import com.faber.api.base.admin.biz.FileSaveBiz;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.base.doc.dto.Track;
import com.faber.api.base.doc.manager.jwt.JwtManager;
import com.faber.api.base.doc.models.enums.DocumentType;
import com.faber.api.base.doc.models.enums.Type;
import com.faber.api.base.doc.models.filemodel.Document;
import com.faber.api.base.doc.models.filemodel.EditorConfig;
import com.faber.api.base.doc.models.filemodel.FileModel;
import com.faber.api.base.doc.utils.FaFileUtility;
import com.faber.core.constant.FaSetting;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Farando
 * @date 2023/3/13 16:33
 * @description
 */
@Service
public class OnlyofficeBiz {

    @Resource
    FaSetting faSetting;

    @Resource
    JwtManager jwtManager;

    @Resource
    FaFileUtility faFileUtility;

    @Resource
    FileSaveBiz fileSaveBiz;

    /**
     * 打开office文件，生成与onlyoffice服务交互的jwt token，返回打开文件的配置
     *
     * @param fileId {@link FileSave#getId()}
     * @return
     */
    public FileModel openFile(String fileId) {
        FileSave fileSave = fileSaveBiz.getById(fileId);

        FileModel fileModel = new FileModel();
        fileModel.setType(Type.desktop);

        DocumentType documentType = faFileUtility.getDocumentType(fileSave.getOriginalFilename());  // get the document type of the specified file

        fileModel.setDocument(new Document());
        fileModel.setEditorConfig(new EditorConfig());

        Document document = fileModel.getDocument();
        document.setTitle(fileSave.getOriginalFilename());
        document.setFileType(fileSave.getExt());
        document.setKey(fileId);
        document.setUrl(faSetting.getOnlyoffice().getCallbackServer() + "/api/base/admin/fileSave/getFile/" + fileId);

        EditorConfig editorConfig = fileModel.getEditorConfig();
        editorConfig.setLang("zh");
        editorConfig.setCallbackUrl(faSetting.getOnlyoffice().getCallbackServer() + "/api/base/doc/onlyoffice/track");

        Map<String, Object> map = new HashMap<>();
        map.put("type", fileModel.getType());
        map.put("documentType", documentType);
        map.put("document", document);
        map.put("editorConfig", editorConfig);

        String token = jwtManager.createToken(map);
        fileModel.setToken(token);

        return fileModel;
    }

    public void track(final Track body) {

    }
}
