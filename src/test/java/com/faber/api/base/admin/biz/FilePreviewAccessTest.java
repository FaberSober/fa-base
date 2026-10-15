package com.faber.api.base.admin.biz;

import com.alibaba.fastjson2.JSON;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaRedisUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FilePreviewAccessTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void validSessionSupportsRangeWithoutLoginAndPreservesNoStoreAfterResponseReset() throws Exception {
        FilePreviewTicketBiz biz = createBiz();
        FilePreviewTicketBiz.PreviewGrant grant = new FilePreviewTicketBiz.PreviewGrant();
        grant.setFileId("file-1");
        grant.setDownloadAllowed(true);
        when(biz.faRedisUtils.getStr("file-preview:session:valid-session")).thenReturn(JSON.toJSONString(grant));
        FileSave file = file();
        when(biz.fileSaveBiz.getById("file-1")).thenReturn(file);
        Path temporaryFile = Files.createTempFile("preview-test-", ".txt");
        try {
            Files.writeString(temporaryFile, "0123456789", StandardCharsets.UTF_8);
            when(biz.fileSaveBiz.getFileObj(file)).thenReturn(temporaryFile.toFile());
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("Range", "bytes=2-5");
            MockHttpServletResponse response = new MockHttpServletResponse();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request, response));

            biz.downloadBySession("valid-session");

            assertEquals(206, response.getStatus());
            assertEquals("2345", response.getContentAsString());
            assertEquals("bytes 2-5/10", response.getHeader("Content-Range"));
            assertEquals("private, no-store", response.getHeader("Cache-Control"));
            assertEquals("no-referrer", response.getHeader("Referrer-Policy"));
            assertFalse(Files.exists(temporaryFile));
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    @Test
    void invalidSessionAndDeniedDownloadNeverReadFile() {
        FilePreviewTicketBiz biz = createBiz();
        assertThrows(BuzzException.class, () -> biz.downloadBySession("expired-session"));
        FilePreviewTicketBiz.PreviewGrant grant = new FilePreviewTicketBiz.PreviewGrant();
        grant.setFileId("file-1");
        grant.setDownloadAllowed(false);
        when(biz.faRedisUtils.getStr("file-preview:session:preview-only")).thenReturn(JSON.toJSONString(grant));
        assertThrows(BuzzException.class, () -> biz.downloadFileBySession("preview-only"));
        verifyNoInteractions(biz.fileSaveBiz);
    }

    @Test
    void exchangedTicketCannotBeReused() {
        FilePreviewTicketBiz biz = createBiz();
        RedissonClient redisson = mock(RedissonClient.class);
        @SuppressWarnings("unchecked")
        RBucket<String> bucket = mock(RBucket.class);
        when(biz.faRedisUtils.getRedisson()).thenReturn(redisson);
        when(biz.faRedisUtils.buildKey("file-preview:ticket:once")).thenReturn("redis-ticket-key");
        when(redisson.<String>getBucket("redis-ticket-key")).thenReturn(bucket);
        FilePreviewTicketBiz.PreviewGrant grant = new FilePreviewTicketBiz.PreviewGrant();
        grant.setFileId("file-1");
        grant.setDownloadAllowed(true);
        when(bucket.getAndDelete()).thenReturn(JSON.toJSONString(grant), null);
        when(biz.fileSaveBiz.getById("file-1")).thenReturn(file());

        assertTrue(biz.exchangeTicket("once").getPreviewUrl().startsWith("/api/base/admin/fileSave/getPreviewFile/"));
        assertThrows(BuzzException.class, () -> biz.exchangeTicket("once"));
    }

    private FilePreviewTicketBiz createBiz() {
        FilePreviewTicketBiz biz = new FilePreviewTicketBiz();
        biz.faRedisUtils = mock(FaRedisUtils.class);
        biz.fileSaveBiz = mock(FileSaveBiz.class);
        return biz;
    }

    private FileSave file() {
        FileSave file = new FileSave();
        file.setId("file-1");
        file.setOriginalFilename("document.txt");
        file.setPlatform("qiniu-1");
        return file;
    }
}
