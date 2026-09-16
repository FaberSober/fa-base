package com.faber.api.base.admin.biz;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.base.admin.vo.ret.FilePreviewResourceRetVo;
import com.faber.api.base.admin.vo.ret.FilePreviewTicketRetVo;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaFileUtils;
import com.faber.core.utils.FaRedisUtils;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 文件预览短时效凭证。
 *
 * <p>启动 ticket 只允许兑换一次，兑换后生成可被 viewer 多次 Range 请求使用的短时效
 * session。这样既避免长期 Token 出现在 H5 URL，也兼容 PDF/Office renderer 的资源请求方式。</p>
 */
@Slf4j
@Service
public class FilePreviewTicketBiz {

    private static final long TICKET_TTL_SECONDS = 180;
    private static final long SESSION_TTL_SECONDS = 600;
    private static final String TICKET_KEY_PREFIX = "file-preview:ticket:";
    private static final String SESSION_KEY_PREFIX = "file-preview:session:";
    private static final String PREVIEW_FILE_PATH = "/api/base/admin/fileSave/getPreviewFile/";

    @Resource FaRedisUtils faRedisUtils;
    @Resource FileSaveBiz fileSaveBiz;

    public FilePreviewTicketRetVo createTicket(String fileId) {
        String normalizedFileId = StrUtil.trim(fileId);
        if (StrUtil.isBlank(normalizedFileId)) {
            throw new BuzzException("文件ID不能为空");
        }

        FileSave fileSave = getFileOrThrow(normalizedFileId);
        String userId = BaseContextHandler.getUserId();
        if (StrUtil.isBlank(userId)) {
            throw new BuzzException("请先登录后再预览文件");
        }

        PreviewGrant grant = new PreviewGrant();
        grant.setFileId(fileSave.getId());
        grant.setUserId(userId);
        grant.setTenantId(BaseContextHandler.getTenantId());
        grant.setWatermarkText(resolveWatermarkText());
        grant.setDownloadAllowed(true);

        String ticket = IdUtil.fastSimpleUUID();
        faRedisUtils.set(ticketKey(ticket), JSON.toJSONString(grant), TICKET_TTL_SECONDS, TimeUnit.SECONDS);
        return new FilePreviewTicketRetVo(ticket, System.currentTimeMillis() + TICKET_TTL_SECONDS * 1000);
    }

    public FilePreviewResourceRetVo exchangeTicket(String ticket) {
        String normalizedTicket = StrUtil.trim(ticket);
        if (StrUtil.isBlank(normalizedTicket)) {
            throw new BuzzException("预览凭证不能为空");
        }

        String rawGrant = faRedisUtils.getRedisson()
                .<String>getBucket(faRedisUtils.buildKey(ticketKey(normalizedTicket)))
                .getAndDelete();
        if (StrUtil.isBlank(rawGrant)) {
            throw new BuzzException("预览凭证无效或已过期");
        }

        PreviewGrant grant;
        try {
            grant = JSON.parseObject(rawGrant, PreviewGrant.class);
        } catch (Exception e) {
            log.warn("文件预览凭证内容无法解析");
            throw new BuzzException("预览凭证无效或已过期");
        }

        FileSave fileSave = getFileOrThrow(grant.getFileId());
        String session = IdUtil.fastSimpleUUID();
        faRedisUtils.set(sessionKey(session), JSON.toJSONString(grant), SESSION_TTL_SECONDS, TimeUnit.SECONDS);

        FilePreviewResourceRetVo result = new FilePreviewResourceRetVo();
        result.setFileId(fileSave.getId());
        result.setFilename(resolveFilename(fileSave));
        result.setContentType(fileSave.getContentType());
        result.setExt(fileSave.getExt());
        result.setSize(fileSave.getSize());
        result.setPreviewUrl(PREVIEW_FILE_PATH + session);
        result.setDownloadUrl("/api/base/admin/fileSave/getPreviewDownload/" + session);
        result.setDownloadAllowed(grant.isDownloadAllowed());
        result.setWatermarkText(grant.getWatermarkText());
        return result;
    }

    public void downloadBySession(String session) throws IOException {
        streamBySession(session, false);
    }

    public void downloadFileBySession(String session) throws IOException {
        streamBySession(session, true);
    }

    private void streamBySession(String session, boolean requireDownloadPermission) throws IOException {
        String normalizedSession = StrUtil.trim(session);
        if (StrUtil.isBlank(normalizedSession)) {
            throw new BuzzException("预览会话不能为空");
        }

        String rawGrant = faRedisUtils.getStr(sessionKey(normalizedSession));
        if (StrUtil.isBlank(rawGrant)) {
            throw new BuzzException("预览会话无效或已过期");
        }

        PreviewGrant grant;
        try {
            grant = JSON.parseObject(rawGrant, PreviewGrant.class);
        } catch (Exception e) {
            log.warn("文件预览会话内容无法解析");
            throw new BuzzException("预览会话无效或已过期");
        }

        if (requireDownloadPermission && !grant.isDownloadAllowed()) {
            throw new BuzzException("当前文件不允许下载");
        }

        FileSave fileSave = getFileOrThrow(grant.getFileId());
        File file = fileSaveBiz.getFileObj(fileSave);
        boolean temporaryFile = !isLocalFile(fileSave);
        try {
            FaFileUtils.downloadFileShard(file, resolveFilename(fileSave));
        } finally {
            if (temporaryFile) {
                FileUtil.del(file);
            }
        }
    }

    private FileSave getFileOrThrow(String fileId) {
        FileSave fileSave = fileSaveBiz.getById(fileId);
        if (fileSave == null) {
            throw new BuzzException("文件不存在或已删除");
        }
        return fileSave;
    }

    private String resolveWatermarkText() {
        String name = BaseContextHandler.getName();
        String username = BaseContextHandler.getUsername();
        if (StrUtil.isNotBlank(name) && StrUtil.isNotBlank(username)) {
            return name + "/" + username;
        }
        return StrUtil.blankToDefault(name, username);
    }

    private String resolveFilename(FileSave fileSave) {
        if (StrUtil.isNotBlank(fileSave.getOriginalFilename())) {
            return fileSave.getOriginalFilename();
        }
        if (StrUtil.isNotBlank(fileSave.getFilename())) {
            return fileSave.getFilename();
        }
        return fileSave.getId();
    }

    private boolean isLocalFile(FileSave fileSave) {
        return StrUtil.startWith(fileSave.getPlatform(), "local-");
    }

    private String ticketKey(String ticket) {
        return TICKET_KEY_PREFIX + ticket;
    }

    private String sessionKey(String session) {
        return SESSION_KEY_PREFIX + session;
    }

    @Data
    public static class PreviewGrant {
        private String fileId;
        private String userId;
        private String tenantId;
        private String watermarkText;
        private boolean downloadAllowed;
    }
}
