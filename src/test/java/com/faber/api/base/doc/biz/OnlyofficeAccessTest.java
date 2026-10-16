package com.faber.api.base.doc.biz;

import cn.hutool.extra.spring.SpringUtil;
import com.faber.api.base.admin.biz.FileSaveBiz;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.base.doc.manager.jwt.JwtManager;
import com.faber.api.base.doc.models.enums.DocumentType;
import com.faber.api.base.doc.models.enums.Mode;
import com.faber.api.base.doc.models.filemodel.Document;
import com.faber.api.base.doc.models.filemodel.EditorConfig;
import com.faber.api.base.doc.models.filemodel.FileModel;
import com.faber.api.base.doc.rest.OnlyofficeController;
import com.faber.api.base.doc.utils.FaFileUtility;
import com.faber.api.base.doc.vo.ret.OpenFileRetVo;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.constant.FaSetting;
import com.faber.core.exception.BuzzException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnlyofficeAccessTest {

    @Test
    void eachOpenUsesFreshAuthorizedUrlInsideDocumentAndJwt() {
        OnlyofficeBiz biz = createBiz();
        FileSave file = new FileSave();
        file.setId("file-1");
        file.setOriginalFilename("文档.docx");
        file.setExt("docx");
        when(biz.fileSaveBiz.getById("file-1")).thenReturn(file);
        when(biz.fileSaveBiz.getFileUrl("https://callback.example.com/", file))
                .thenReturn("https://files.example.com/document.docx?e=first&token=test",
                        "https://files.example.com/document.docx?e=second&token=test");
        when(biz.faFileUtility.getDocumentType("文档.docx")).thenReturn(DocumentType.word);
        when(biz.jwtManager.createToken(anyMap())).thenAnswer(invocation -> {
            Map<String, Object> payload = invocation.getArgument(0);
            Document document = (Document) payload.get("document");
            assertTrue(document.getUrl().contains("&token=test"));
            return "test-jwt";
        });
        try (MockedStatic<SpringUtil> spring = mockStatic(SpringUtil.class)) {
            spring.when(() -> SpringUtil.getBean(FileModel.class)).thenAnswer(invocation -> {
                FileModel model = new FileModel();
                model.setDocument(new Document());
                model.setEditorConfig(new EditorConfig());
                return model;
            });
            FileModel first = biz.openFile("file-1", Mode.edit).getFileModel();
            FileModel second = biz.openFile("file-1", Mode.edit).getFileModel();
            assertTrue(first.getDocument().getUrl().contains("e=first"));
            assertTrue(second.getDocument().getUrl().contains("e=second"));
            assertEquals("file-1", first.getDocument().getKey());
            assertEquals("文档.docx", first.getDocument().getTitle());
            assertEquals("https://callback.example.com/api/base/doc/onlyoffice/track", first.getEditorConfig().getCallbackUrl());
            assertEquals("test-jwt", first.getToken());
            verify(biz.jwtManager, times(2)).createToken(anyMap());
        }
    }

    @Test
    void missingDocumentFailsBeforeBuildingOrSigningConfiguration() {
        OnlyofficeBiz biz = createBiz();
        assertThrows(BuzzException.class, () -> biz.openFileModal("missing", Mode.edit));
        verifyNoInteractions(biz.faFileUtility, biz.jwtManager);
    }

    @Test
    void openEndpointRequiresLoginAndPreventsCachingAndResponseLogging() throws Exception {
        OnlyofficeController controller = new OnlyofficeController();
        OnlyofficeBiz biz = mock(OnlyofficeBiz.class);
        OpenFileRetVo result = new OpenFileRetVo();
        when(biz.openFile("file-1", Mode.edit)).thenReturn(result);
        ReflectionTestUtils.setField(controller, "onlyofficeBiz", biz);
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertSame(result, controller.openFile("file-1", "edit", response).getData());
        assertEquals("private, no-store", response.getHeader("Cache-Control"));
        assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
        var endpoint = OnlyofficeController.class.getMethod("openFile", String.class, String.class, HttpServletResponse.class);
        assertNotNull(endpoint.getAnnotation(LogNoRet.class));
        assertNull(endpoint.getAnnotation(IgnoreUserToken.class));
    }

    private OnlyofficeBiz createBiz() {
        OnlyofficeBiz biz = new OnlyofficeBiz();
        biz.fileSaveBiz = mock(FileSaveBiz.class);
        biz.faFileUtility = mock(FaFileUtility.class);
        biz.jwtManager = mock(JwtManager.class);
        biz.faSetting = new FaSetting();
        FaSetting.Onlyoffice office = new FaSetting.Onlyoffice();
        office.setCallbackServer("https://callback.example.com/");
        office.setOnlyofficeServer("https://office.example.com/");
        biz.faSetting.setOnlyoffice(office);
        return biz;
    }
}
