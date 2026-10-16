package com.faber.api.base.admin.biz;

import cn.hutool.http.HttpUtil;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.core.constant.FaSetting;
import com.faber.core.enums.ConfigSysQiniuAccessTypeEnum;
import com.faber.core.enums.ConfigSysStorageActiveEnum;
import com.faber.core.exception.BuzzException;
import com.faber.core.service.ConfigSysService;
import com.faber.core.vo.config.FaConfig;
import com.qiniu.util.Auth;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.LocalPlusFileStorage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class FileSaveAccessTest {

    private FileSaveBiz biz;
    private FaConfig config;
    private FileSave file;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        config = new FaConfig();
        config.setStoreActive(ConfigSysStorageActiveEnum.LOCAL_PLUS);
        config.setQiniuAccessType(ConfigSysQiniuAccessTypeEnum.PRIVATE);
        config.setQiniuAk("test-ak");
        config.setQiniuSk("test-sk");
        config.setQiniuDomain("https://files.example.com/assets/");
        config.setQiniuBasePath("prod/");
        ConfigSysService configs = mock(ConfigSysService.class);
        when(configs.getConfig()).thenReturn(config);
        biz = spy(new FileSaveBiz());
        biz.configSysService = configs;
        biz.faSetting = new FaSetting();
        FaSetting.Url url = new FaSetting.Url();
        url.setServerHost("https://app.example.com/");
        biz.faSetting.setUrl(url);
        file = new FileSave();
        file.setId("file-1");
        file.setPlatform("qiniu-1");
        file.setUrl("https://files.example.com/assets/prod/中文 文档.txt");
        file.setFilename("document.txt");
        file.setOriginalFilename("文档.txt");
        file.setExt("txt");
        file.setSize(100L);
        doReturn(file).when(biz).getById("file-1");
        bindResponse();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void signsEncodedOriginalForTenMinutesEvenWhenAnotherStorageIsActive() throws Exception {
        biz.downloadFileById("file-1");
        assertSignedUrl("https://files.example.com/assets/prod/%E4%B8%AD%E6%96%87%20%E6%96%87%E6%A1%A3.txt");
        assertEquals("private, no-store", response.getHeader("Cache-Control"));
        assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
    }

    @Test
    void signsThumbnailIncludingImageProcessingParameters() throws Exception {
        file.setThUrl("https://files.example.com/assets/prod/thumb.jpg?imageView2/1/w/100/h/100");
        file.setThSize(50L);
        biz.getFilePreview("file-1");
        assertSignedUrl(file.getThUrl());
    }

    @Test
    void previewFallsBackToOriginalWhenThumbnailIsMissingOrLarger() throws Exception {
        file.setUrl("https://files.example.com/assets/prod/original.jpg");
        biz.getFilePreview("file-1");
        assertSignedUrl(file.getUrl());
        bindResponse();
        file.setThUrl("https://files.example.com/assets/prod/too-large.jpg");
        file.setThSize(101L);
        biz.getFilePreview("file-1");
        assertSignedUrl(file.getUrl());
    }

    @Test
    void rejectsForeignUrlsTraversalAndPersistedDownloadCredentials() {
        for (String url : new String[]{
                "https://files.example.com.evil.test/assets/prod/a.txt",
                "https://files.example.com@evil.test/assets/prod/a.txt",
                "http://files.example.com/assets/prod/a.txt",
                "https://files.example.com/assets/production/a.txt",
                "https://files.example.com/assets/prod/../other.txt",
                "https://files.example.com/assets/prod/%2e%2e/other.txt",
                "https://files.example.com/assets/prod/%5cother.txt",
                "https://files.example.com/assets/prod/%0d%0a.txt",
                "https://files.example.com/assets/prod/a.txt#fragment",
                "https://files.example.com/assets/prod/a.txt?token=old",
                "https://files.example.com/assets/prod/a.txt?imageView2/1&e=123",
                "https://files.example.com/assets/prod/a.txt?%74oken=old"}) {
            file.setUrl(url);
            assertThrows(BuzzException.class, () -> biz.downloadFileById("file-1"), url);
            assertNull(response.getRedirectedUrl());
        }
    }

    @Test
    void preservesPublicAndOtherStorageRedirects() throws Exception {
        file.setUrl("https://files.example.com/assets/prod/plain.txt");
        config.setQiniuAccessType(ConfigSysQiniuAccessTypeEnum.PUBLIC);
        biz.downloadFileById("file-1");
        assertEquals(file.getUrl(), response.getRedirectedUrl());
        assertNull(response.getHeader("Cache-Control"));
        bindResponse();
        config.setQiniuAccessType(ConfigSysQiniuAccessTypeEnum.PRIVATE);
        file.setPlatform("minio-1");
        assertFalse(biz.isPrivateQiniu(file));
        biz.downloadFileById("file-1");
        assertEquals(file.getUrl(), response.getRedirectedUrl());
    }

    @Test
    void onlyConfiguredSystemResourcesArePublic() {
        assertFalse(biz.isPublicSystemResource("file-1"));
        assertFalse(biz.isPublicSystemResource(null));
        config.setLoginBg("file-1");
        assertTrue(biz.isPublicSystemResource("file-1"));
        assertFalse(biz.isPublicSystemResource("another-file"));
    }

    @Test
    void privateTextUsesSignedDownloadAndDeletesTemporaryFile() {
        AtomicReference<File> downloaded = new AtomicReference<>();
        try (MockedStatic<HttpUtil> http = mockStatic(HttpUtil.class)) {
            http.when(() -> HttpUtil.downloadFile(anyString(), any(File.class))).thenAnswer(invocation -> {
                assertTrue(invocation.<String>getArgument(0).contains("&token="));
                File target = invocation.getArgument(1);
                downloaded.set(target);
                Files.writeString(target.toPath(), "私有文本", StandardCharsets.UTF_8);
                return target.length();
            });
            assertEquals("私有文本", biz.getFileStr("file-1"));
            assertNotNull(downloaded.get());
            assertFalse(downloaded.get().exists());
        }
    }

    @Test
    void privateDownloadFailureDoesNotLeakSignedUrlAndDeletesTemporaryFile() {
        AtomicReference<File> downloaded = new AtomicReference<>();
        try (MockedStatic<HttpUtil> http = mockStatic(HttpUtil.class)) {
            http.when(() -> HttpUtil.downloadFile(anyString(), any(File.class))).thenAnswer(invocation -> {
                downloaded.set(invocation.getArgument(1));
                throw new IllegalStateException("request failed " + invocation.<String>getArgument(0));
            });
            BuzzException failure = assertThrows(BuzzException.class, () -> biz.getFileObj(file));
            assertFalse(failure.getMessage().contains("token="));
            assertNull(failure.getCause());
            assertNotNull(downloaded.get());
            assertFalse(downloaded.get().exists());
        }
    }

    @Test
    void internalUrlEntryPointsSignPrivateFilesWithoutPersistingCredentialsOrRequiringHttpContext() {
        RequestContextHolder.resetRequestAttributes();
        String original = file.getUrl();
        String unsigned = "https://files.example.com/assets/prod/%E4%B8%AD%E6%96%87%20%E6%96%87%E6%A1%A3.txt";
        assertSignedUrl(unsigned, biz.getFileUrl("file-1"));
        assertSignedUrl(unsigned, biz.getFileUrl("https://callback.example.com/", "file-1"));
        assertSignedUrl(unsigned, biz.getFileUrl("https://callback.example.com/", file));
        assertSignedUrl(unsigned, biz.getFileFullPath("file-1"));
        assertEquals(original, file.getUrl());
    }

    @Test
    void internalUrlsPreservePublicRemoteAndLocalPathSemantics() {
        config.setQiniuAccessType(ConfigSysQiniuAccessTypeEnum.PUBLIC);
        assertEquals(file.getUrl(), biz.getFileUrl("file-1"));
        assertEquals(file.getUrl(), biz.getFileFullPath("file-1"));
        config.setQiniuAccessType(ConfigSysQiniuAccessTypeEnum.PRIVATE);
        file.setPlatform("minio-1");
        assertEquals(file.getUrl(), biz.getFileUrl(null, file));
        file.setPlatform("local-plus-1");
        assertEquals("https://app.example.com/api/base/admin/fileSave/getFile/file-1", biz.getFileUrl("file-1"));
        assertEquals("https://callback.example.com/api/base/admin/fileSave/getFile/file-1", biz.getFileUrl("https://callback.example.com/", file));
        assertEquals("/api/base/admin/fileSave/getFile/file-1", biz.getFileUrl(null, file));
        biz.fileStorageService = mock(FileStorageService.class);
        LocalPlusFileStorage local = mock(LocalPlusFileStorage.class);
        when(biz.fileStorageService.getFileStorage("local-plus-1")).thenReturn(local);
        when(local.getAbsolutePath(file.getUrl())).thenReturn("/tmp/stored/document.txt");
        assertEquals("/tmp/stored/document.txt", biz.getFileFullPath("file-1"));
    }

    @Test
    void internalUrlEntryPointsRejectMissingFiles() {
        doReturn(null).when(biz).getById("missing");
        assertThrows(BuzzException.class, () -> biz.getFileUrl("missing"));
        assertThrows(BuzzException.class, () -> biz.getFileFullPath("missing"));
    }

    private void assertSignedUrl(String unsignedUrl) {
        assertSignedUrl(unsignedUrl, response.getRedirectedUrl());
    }

    private void assertSignedUrl(String unsignedUrl, String signed) {
        assertNotNull(signed);
        assertTrue(signed.startsWith(unsignedUrl + (unsignedUrl.contains("?") ? "&" : "?") + "e="));
        int deadlineStart = unsignedUrl.length() + 3;
        long deadline = Long.parseLong(signed.substring(deadlineStart, signed.indexOf("&token=", deadlineStart)));
        long remaining = deadline - System.currentTimeMillis() / 1000;
        assertTrue(remaining >= 598 && remaining <= 600);
        assertEquals(Auth.create("test-ak", "test-sk").privateDownloadUrlWithDeadline(unsignedUrl, deadline), signed);
    }

    private void bindResponse() {
        response = new MockHttpServletResponse();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest(), response));
    }
}
