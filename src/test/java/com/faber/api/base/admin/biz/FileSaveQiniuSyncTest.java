package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.base.admin.mapper.FileSaveMapper;
import com.faber.api.base.admin.rest.FileSaveController;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.constant.FaSetting;
import com.faber.core.enums.ConfigSysQiniuAccessTypeEnum;
import com.faber.core.exception.BuzzException;
import com.faber.core.service.ConfigSysService;
import com.faber.core.utils.FaFileUploadUtils;
import com.faber.core.vo.config.FaConfig;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.model.FileInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FileSaveQiniuSyncTest {

    private FileSaveBiz biz;
    private FileSaveMapper mapper;
    private FaConfig config;
    private ConfigSysService configs;
    private static final String NAME = "my_file v1_20261015000100_123.txt";
    private static final String KEY = "prod/file/2026/10/15/" + NAME;
    private static final String URL = "https://files.example.com/assets/" + KEY;

    @BeforeEach
    void setUp() {
        config = new FaConfig();
        config.setQiniuAccessType(ConfigSysQiniuAccessTypeEnum.PRIVATE);
        config.setQiniuAk("test-ak");
        config.setQiniuSk("test-sk");
        config.setQiniuBucketName("test-bucket");
        config.setQiniuDomain("https://files.example.com/assets/");
        config.setQiniuBasePath("prod/");
        configs = mock(ConfigSysService.class);
        when(configs.getConfig()).thenReturn(config);
        mapper = mock(FileSaveMapper.class);
        when(mapper.insert(any(FileSave.class))).thenReturn(1);
        biz = new FileSaveBiz();
        biz.configSysService = configs;
        biz.faSetting = new FaSetting();
        biz.faSetting.setFile(new FaSetting.File());
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);
    }

    @Test
    void bothAccessTypesRegisterSdkMetadataWithStableUrlAndCompleteOriginalName() throws Exception {
        for (ConfigSysQiniuAccessTypeEnum type : ConfigSysQiniuAccessTypeEnum.values()) {
            config.setQiniuAccessType(type);
            clearInvocations(mapper);
            FileInfo metadata = metadata(321, "text/plain");
            try (MockedConstruction<BucketManager> managers = mockConstruction(BucketManager.class,
                    (manager, context) -> when(manager.stat("test-bucket", KEY)).thenReturn(metadata))) {
                FileSave saved = biz.syncUrlQiniu(URL);
                verify(managers.constructed().get(0)).stat("test-bucket", KEY);
                ArgumentCaptor<FileSave> record = ArgumentCaptor.forClass(FileSave.class);
                verify(mapper).insert(record.capture());
                assertSame(saved, record.getValue());
                assertEquals("123", saved.getId());
                assertEquals("my_file v1.txt", saved.getOriginalFilename());
                assertEquals(NAME, saved.getFilename());
                assertEquals("prod/", saved.getBasePath());
                assertEquals("file/2026/10/15/", saved.getPath());
                assertEquals("txt", saved.getExt());
                assertEquals(321L, saved.getSize());
                assertEquals("text/plain", saved.getContentType());
                assertEquals("qiniu-1", saved.getPlatform());
                assertEquals(URL.replace(" ", "%20"), saved.getUrl());
                assertFalse(saved.getUrl().contains("token="));
            }
        }
    }

    @Test
    void supportsEncodedUnicodeAndExtensionlessZeroByteFiles() throws Exception {
        String key = "prod/read_me_20261015000100_42";
        try (MockedConstruction<BucketManager> managers = mockConstruction(BucketManager.class,
                (manager, context) -> when(manager.stat(anyString(), anyString())).thenReturn(metadata(0, null)))) {
            FileSave saved = biz.syncUrlQiniu("https://files.example.com/assets/" + key);
            assertEquals("42", saved.getId());
            assertEquals("read_me", saved.getOriginalFilename());
            assertEquals("", saved.getPath());
            assertEquals(0L, saved.getSize());
            assertEquals("application/octet-stream", saved.getContentType());
            saved = biz.syncUrlQiniu("https://files.example.com/assets/prod/%E4%B8%AD%E6%96%87%20v1_20261015000100_43.txt");
            assertEquals("中文 v1.txt", saved.getOriginalFilename());
            verify(managers.constructed().get(1)).stat("test-bucket", "prod/中文 v1_20261015000100_43.txt");
        }
    }

    @Test
    void rejectsForeignPathsQueriesAndMalformedNamesBeforeContactingQiniu() {
        try (MockedConstruction<BucketManager> managers = mockConstruction(BucketManager.class)) {
            for (String url : new String[]{
                    URL.replace("files.example.com", "evil.example.com"),
                    URL.replace("https:", "http:"),
                    URL.replace("/prod/", "/production/"),
                    URL.replace("/file/", "/../"),
                    URL + "?imageView2/1/w/100", URL + "?e=123&token=old", URL + "#fragment",
                    "https://files.example.com/assets/prod/plain.txt",
                    "https://files.example.com/assets/prod/plain_20261015000100_not-an-id.txt"}) {
                assertThrows(BuzzException.class, () -> biz.syncUrlQiniu(url), url);
            }
            assertTrue(managers.constructed().isEmpty());
            verify(mapper, never()).insert(any(FileSave.class));
        }
    }

    @Test
    void rejectsDisallowedFileTypesBeforeReadingMetadata() {
        biz.faSetting.getFile().setAllowFiles("jpg,png");
        try (MockedConstruction<BucketManager> managers = mockConstruction(BucketManager.class)) {
            assertThrows(BuzzException.class, () -> biz.syncUrlQiniu(URL));
            assertTrue(managers.constructed().isEmpty());
            verify(mapper, never()).insert(any(FileSave.class));
        }
    }

    @Test
    void missingObjectDoesNotCreateRecordOrExposeUpstreamError() {
        try (MockedConstruction<BucketManager> managers = mockConstruction(BucketManager.class,
                (manager, context) -> when(manager.stat(anyString(), anyString()))
                        .thenThrow(new QiniuException(new IOException("upstream-private-details"))))) {
            BuzzException failure = assertThrows(BuzzException.class, () -> biz.syncUrlQiniu(URL));
            assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains("upstream-private-details"));
            verify(mapper, never()).insert(any(FileSave.class));
        }
    }

    @Test
    void failedDatabaseWriteIsNotReportedAsSuccessfulRegistration() {
        when(mapper.insert(any(FileSave.class))).thenReturn(0);
        try (MockedConstruction<BucketManager> managers = mockConstruction(BucketManager.class,
                (manager, context) -> when(manager.stat(anyString(), anyString())).thenReturn(metadata(1, "text/plain")))) {
            assertThrows(BuzzException.class, () -> biz.syncUrlQiniu(URL));
        }
    }

    @Test
    void uploadTokenReportsAccessTypeAndBothEndpointsRemainAuthenticated() throws Exception {
        FileSaveController controller = new FileSaveController();
        ReflectionTestUtils.setField(controller, "configSysService", configs);
        try (MockedStatic<FaFileUploadUtils> uploads = mockStatic(FaFileUploadUtils.class)) {
            uploads.when(() -> FaFileUploadUtils.getQiniuUploadToken("test-ak", "test-sk", "test-bucket")).thenReturn("test-upload-token");
            assertEquals("PRIVATE", controller.getQiniuUploadToken().getData().getString("accessType"));
        }
        assertNull(FileSaveController.class.getMethod("getQiniuUploadToken").getAnnotation(IgnoreUserToken.class));
        assertNull(FileSaveController.class.getMethod("syncUrlQiniu", Map.class).getAnnotation(IgnoreUserToken.class));
    }

    private FileInfo metadata(long size, String mime) {
        FileInfo metadata = new FileInfo();
        metadata.fsize = size;
        metadata.mimeType = mime;
        return metadata;
    }
}
