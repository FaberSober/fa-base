package com.faber.api.base.admin.biz;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.http.HttpUtil;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.faber.api.base.admin.entity.FileSave;
import com.faber.api.base.admin.mapper.FileSaveMapper;
import com.faber.core.bean.BaseCrtEntity;
import com.faber.core.constant.FaSetting;
import com.faber.core.enums.ConfigSysStorageActiveEnum;
import com.faber.core.enums.ConfigSysQiniuAccessTypeEnum;
import com.faber.core.exception.BuzzException;
import com.faber.core.service.ConfigSysService;
import com.faber.core.service.StorageService;
import com.faber.core.utils.FaDateUtils;
import com.faber.core.utils.FaFileUtils;
import com.faber.core.vo.config.FaConfig;
import com.faber.core.web.biz.BaseBiz;
import com.qiniu.util.Auth;
import com.qiniu.common.QiniuException;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageProperties;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.FileStorageServiceBuilder;
import org.dromara.x.file.storage.core.platform.FileStorage;
import org.dromara.x.file.storage.core.platform.LocalPlusFileStorage;
import org.dromara.x.file.storage.core.upload.UploadPretreatment;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;


/**
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2019-08-19 10:09:36
 */
@Slf4j
@Service
public class FileSaveBiz extends BaseBiz<FileSaveMapper, FileSave> implements StorageService {

    private static final Pattern QINIU_FILENAME = Pattern.compile("^(.+)_\\d{14}_(\\d+)(\\.[^/]*)?$");

    @Resource FileStorageService fileStorageService;
    @Resource ConfigSysService configSysService;
    @Resource FaSetting faSetting;
    @Resource FileBizBiz fileBizBiz;

    public boolean isPrivateQiniu(FileSave file) {
        return file != null && ConfigSysStorageActiveEnum.QINIU.getDesc().equals(file.getPlatform())
                && configSysService.getConfig().getQiniuAccessType() == ConfigSysQiniuAccessTypeEnum.PRIVATE;
    }

    public boolean isPublicSystemResource(String fileId) {
        if (StrUtil.isBlank(fileId)) return false;
        FaConfig config = configSysService.getConfig();
        return fileId.equals(config.getLogo()) || fileId.equals(config.getLogoWithText())
                || fileId.equals(config.getLoginBg()) || fileId.equals(config.getWxminiapp());
    }

    private String getRemoteFileUrl(FileSave file, boolean preview) {
        String url = preview && hasTh(file) ? file.getThUrl() : file.getUrl();
        if (!isPrivateQiniu(file)) return URLUtil.encode(url);
        FaConfig config = configSysService.getConfig();
        if (StrUtil.hasBlank(config.getQiniuAk(), config.getQiniuSk(), config.getQiniuDomain())) {
            throw new BuzzException("七牛云私有访问配置不完整");
        }
        // 存储组件的七牛预签名接口不保留处理参数，直接复用七牛 SDK 对完整 URL 签名。
        return Auth.create(config.getQiniuAk(), config.getQiniuSk())
                .privateDownloadUrl(validateQiniuUrl(url, config), 600);
    }

    private String validateQiniuUrl(String url, FaConfig config) {
        try {
            URI resource = URI.create(url.replace(" ", "%20"));
            URI domain = URI.create(config.getQiniuDomain());
            String prefix = domain.getPath();
            if (!prefix.endsWith("/")) prefix += "/";
            String basePath = StrUtil.nullToEmpty(config.getQiniuBasePath());
            prefix += basePath.startsWith("/") ? basePath.substring(1) : basePath;
            if (!prefix.endsWith("/")) prefix += "/";
            String path = resource.getPath();
            if (!("http".equalsIgnoreCase(resource.getScheme()) || "https".equalsIgnoreCase(resource.getScheme()))
                    || !resource.getScheme().equalsIgnoreCase(domain.getScheme())
                    || resource.getHost() == null || !resource.getHost().equalsIgnoreCase(domain.getHost())
                    || resource.getPort() != domain.getPort() || resource.getRawUserInfo() != null
                    || resource.getRawFragment() != null || domain.getRawUserInfo() != null
                    || domain.getRawQuery() != null || domain.getRawFragment() != null
                    || path == null || !path.startsWith(prefix) || path.length() <= prefix.length()
                    || path.contains("\\") || path.chars().anyMatch(Character::isISOControl)) {
                throw new IllegalArgumentException();
            }
            for (String part : path.split("/")) {
                if (".".equals(part) || "..".equals(part)) throw new IllegalArgumentException();
            }
            if (resource.getRawQuery() != null) {
                String query = URLDecoder.decode(resource.getRawQuery(), StandardCharsets.UTF_8);
                for (String part : query.split("&")) {
                    if (part.startsWith("e=") || part.startsWith("token=")) throw new IllegalArgumentException();
                }
            }
            return resource.toASCIIString();
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BuzzException("七牛文件地址无效或不在配置的存储路径内");
        }
    }

    /**
     * 下载URL文件到本地，并入库
     *
     * @param url
     * @param filename
     * @return
     */
    public FileSave download(String url, String filename) throws IOException {
        String name = filename.substring(0, filename.lastIndexOf("."));
        String ext = filename.substring(filename.lastIndexOf("."));
        File tmpFile = FileUtil.createTempFile(name, ext, true);
        HttpUtil.downloadFile(url, tmpFile);
        FileSave fileSave = upload(tmpFile);
        if (fileSave != null) {
            lambdaUpdate()
                    .set(FileSave::getOutUrl, url)
                    .eq(FileSave::getId, fileSave.getId())
                    .update();
        }
        return fileSave;
    }

    /**
     * 下载URL文件到本地，并入库
     *
     * @param url
     * @param filename
     * @param force    force to redownload
     * @return
     */
    public FileSave download(String url, String filename, boolean force) throws IOException {
        if (!force) {
            FileSave cacheFileSave = getTop(
                    lambdaQuery().eq(FileSave::getOutUrl, url).orderByDesc(BaseCrtEntity::getCrtTime)
            );
            if (cacheFileSave != null) {
                return cacheFileSave;
            }
        }
        return download(url, filename);
    }

    /**
     * 保存已经上传到物理存储的文件记录。
     */
    private FileSave saveUploadedFile(FileInfo fileInfo, String id, String md5) {
        FileSave fileSave = new FileSave();
        BeanUtil.copyProperties(fileInfo, fileSave);
        fileSave.setId(id);
        fileSave.setMd5(md5);
        try {
            if (!super.save(fileSave)) {
                throw new BuzzException("文件记录保存失败");
            }
            return fileSave;
        } catch (RuntimeException e) {
            cleanupStorage(fileInfo);
            throw e;
        }
    }

    private void cleanupStorage(FileInfo fileInfo) {
        try {
            if (fileInfo != null && fileStorageService.exists(fileInfo)) {
                fileStorageService.delete(fileInfo);
            }
        } catch (Exception e) {
            log.error("清理上传文件失败, url={}", fileInfo == null ? null : fileInfo.getUrl(), e);
        }
    }

    /**
     * 清理尚未建立业务关联的上传文件。调用方的数据库事务负责回滚文件记录。
     */
    public void cleanupUploadedFile(FileSave fileSave) {
        if (fileSave == null) return;
        FileInfo fileInfo = getFileInfo(fileSave);
        if (fileStorageService.exists(fileInfo) && !fileStorageService.delete(fileInfo)) {
            throw new BuzzException("上传文件清理失败");
        }
    }

    /**
     * 上传文件
     *
     * @param file
     * @return
     * @throws IOException
     */
    public FileSave upload(MultipartFile file) {
        UploadPretreatment uploadPretreatment = fileStorageService.of(file).setPlatform(faSetting.getFile().getSavePlatform());

        String extName = FileUtil.extName(file.getOriginalFilename());
        if (FaFileUtils.isImg(extName)) {
            try {
                // 检查图片尺寸，只有当宽高都大于等于200时才生成缩略图
                BufferedImage image = ImageIO.read(file.getInputStream());
                if (image != null && image.getWidth() >= 200 && image.getHeight() >= 200) {
                    uploadPretreatment = uploadPretreatment.thumbnail(th -> th.size(200, 200));  //生成一张 200*200 的缩略图（这里操作缩略图）;
                }
            } catch (Exception e) {
                log.error(e.getMessage() + ", fileName=" + file.getOriginalFilename(), e);
            }
        }

        // 检查文件上传类型
        if (!faSetting.getFile().isFileAllowed(file.getOriginalFilename())) {
            log.error("脚本攻击, 非法文件，附件上传出错:" + file.getOriginalFilename());
            throw new BuzzException("非法文件，附件上传出错");
        }

//        String md5 = DigestUtil.md5Hex(file.getBytes());
        String dir = "file" + "/" + DateUtil.thisYear() + "/" + FaDateUtils.thisMonth() + "/" + DateUtil.thisDayOfMonth() + "/";
        String id = IdWorker.getId() + "";
        FileInfo fileInfo = uploadPretreatment
                .setPath(dir)
                .setSaveFilename(FaFileUtils.addTsAndIdToFileName(file.getOriginalFilename(), id))
                .upload();
        return saveUploadedFile(fileInfo, id, null);
    }

    /**
     * URL上传文件
     *
     * @param url
     * @return
     */
    public FileSave uploadFromUrl(String url) {
        String fullName = url.substring(url.lastIndexOf("/") + 1);
        int dot = fullName.lastIndexOf(".");
        String name = fullName.substring(0, dot);
        String suffix = fullName.substring(dot);

        File tmpFile = FileUtil.createTempFile(name, suffix, false);
        HttpUtil.downloadFile(url, tmpFile);
        return upload(tmpFile);
    }

    /**
     * 同步七牛云URL信息
     * @param url 形如：http://www.fa.top/base_path/modal_path/2026/01/07/ikun_20260107153219_2008803872863117313.mp4
     * @return
     */
    public FileSave syncUrlQiniu(String url) {
        FaConfig faConfig = configSysService.getConfig();
        URI resource = URI.create(validateQiniuUrl(url, faConfig));
        if (resource.getRawQuery() != null) throw new BuzzException("七牛文件登记地址不能包含查询参数");
        if (StrUtil.hasBlank(faConfig.getQiniuAk(), faConfig.getQiniuSk(), faConfig.getQiniuBucketName())) {
            throw new BuzzException("七牛云配置不完整");
        }
        String domainPath = URI.create(faConfig.getQiniuDomain()).getPath();
        if (!domainPath.endsWith("/")) domainPath += "/";
        String key = resource.getPath().substring(domainPath.length());
        String fullName = key.substring(key.lastIndexOf('/') + 1);
        Matcher filename = QINIU_FILENAME.matcher(fullName);
        if (!filename.matches()) throw new BuzzException("七牛文件名必须包含上传时间和文件ID");
        String originalFilename = filename.group(1) + StrUtil.nullToEmpty(filename.group(3));
        if (!faSetting.getFile().isFileAllowed(originalFilename)) {
            throw new BuzzException("非法文件，附件上传出错");
        }
        String basePath = StrUtil.removePrefix(StrUtil.nullToEmpty(faConfig.getQiniuBasePath()), "/");
        if (!basePath.isEmpty() && !basePath.endsWith("/")) basePath += "/";

        com.qiniu.storage.model.FileInfo metadata;
        try {
            BucketManager manager = new BucketManager(Auth.create(faConfig.getQiniuAk(), faConfig.getQiniuSk()),
                    new Configuration(Region.autoRegion()));
            metadata = manager.stat(faConfig.getQiniuBucketName(), key);
        } catch (QiniuException e) {
            throw new BuzzException("七牛文件不存在或元数据读取失败，请检查存储配置");
        }
        if (metadata == null || metadata.fsize < 0) throw new BuzzException("七牛文件元数据无效");

        FileSave fileSave = new FileSave();
        fileSave.setId(filename.group(2));
        fileSave.setUrl(resource.toASCIIString());
        fileSave.setSize(metadata.fsize);
        fileSave.setFilename(fullName);
        fileSave.setOriginalFilename(originalFilename);
        fileSave.setBasePath(basePath);
        fileSave.setPath(key.substring(basePath.length(), key.length() - fullName.length()));
        fileSave.setExt(FileUtil.extName(fullName));
        fileSave.setContentType(StrUtil.blankToDefault(metadata.mimeType, "application/octet-stream"));
        fileSave.setPlatform(ConfigSysStorageActiveEnum.QINIU.getDesc());
        fileSave.setAttr("{}");
        if (!super.save(fileSave)) throw new BuzzException("七牛文件登记失败");
        return fileSave;
    }

    /**
     * 上传文件
     *
     * @param file
     * @return
     */
    public FileSave upload(File file) {
        UploadPretreatment uploadPretreatment = fileStorageService.of(file).setPlatform(faSetting.getFile().getSavePlatform());

        String extName = FileNameUtil.extName(file.getName());
        if (FaFileUtils.isImg(extName)) {
            try {
                // 检查图片尺寸，只有当宽高都大于等于200时才生成缩略图
                BufferedImage image = ImageIO.read(file);
                if (image != null && image.getWidth() >= 200 && image.getHeight() >= 200) {
                    uploadPretreatment = uploadPretreatment.thumbnail(th -> th.size(200, 200));  //生成一张 200*200 的缩略图（这里操作缩略图）;
                }
            } catch (Exception e) {
                log.error(e.getMessage() + ", fileName=" + file.getName(), e);
            }
        }

        String md5 = DigestUtil.md5Hex(file);

        String dir = "file" + "/" + DateUtil.thisYear() + "/" + FaDateUtils.thisMonth() + "/" + DateUtil.thisDayOfMonth() + "/";
        String id = IdWorker.getId() + "";
        FileInfo fileInfo = uploadPretreatment
                .setPath(dir)
                .setSaveFilename(FaFileUtils.addTsAndIdToFileName(file.getName(), id))
                .upload();
        return saveUploadedFile(fileInfo, id, md5);
    }

    public File getFileObj(FileSave fileSave) {
        if (fileSave == null) {
            throw new BuzzException("File Not Found");
        }
        // 本地存储
        if (fileSave.getPlatform().startsWith("local-")) {
            LocalPlusFileStorage storage = ((LocalPlusFileStorage) fileStorageService.getFileStorage("local-plus-1"));
            String fileFullPath = storage.getAbsolutePath(fileSave.getUrl());

            return new File(fileFullPath);
        } else {
            File tempFile = FileUtil.createTempFile(fileSave.getFileNameWithoutExtension(), "." + fileSave.getExt(), true);
            try {
                if (isPrivateQiniu(fileSave)) {
                    HttpUtil.downloadFile(getRemoteFileUrl(fileSave, false), tempFile);
                } else {
                    fileStorageService.download(getFileInfo(fileSave)).file(tempFile);
                }
                return tempFile;
            } catch (RuntimeException e) {
                FileUtil.del(tempFile);
                // 网络异常可能含下载签名，不能把原异常写入日志或响应。
                if (isPrivateQiniu(fileSave)) throw new BuzzException("七牛文件下载失败，请检查存储配置或文件状态");
                throw e;
            }
        }
    }

    /**
     * 通过fileId获取文件
     *
     * @param fileId
     * @return
     */
    public File getFileObj(String fileId) {
        FileSave fileSave = getById(fileId);
        return this.getFileObj(fileSave);
    }

    public File getFileObjWithOriginalName(String fileId) throws IOException {
        FileSave fileSave = getById(fileId);
        File file = this.getFileObj(fileSave);
        File dir = jodd.io.FileUtil.createTempDirectory();
        File tmpFile = new File(dir.getAbsolutePath() + File.separator + fileSave.getOriginalFilename());
        jodd.io.FileUtil.copyFile(file, tmpFile);
        return tmpFile;
    }

    /**
     * 通过fileId，下载文件到http返回流。下载使用分片下载。
     *
     * @param fileId
     * @throws IOException
     */
    public void downloadFileById(String fileId) throws IOException {
        FileSave fileSave = getById(fileId);
        if (fileSave == null) {
            return;
        }

        // 本地存储
        if (fileSave.getPlatform().startsWith("local-")) {
            LocalPlusFileStorage storage = ((LocalPlusFileStorage) fileStorageService.getFileStorage("local-plus-1"));
            String fileFullPath = storage.getAbsolutePath(fileSave.getUrl());

            FaFileUtils.downloadFileShard(new File(fileFullPath), fileSave.getOriginalFilename());
        } else {
            // TO-DO 其他下载渠道直接返回URL
            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
            if (isPrivateQiniu(fileSave)) {
                response.setHeader("Cache-Control", "private, no-store");
                response.setHeader("Referrer-Policy", "no-referrer");
            }
            response.sendRedirect(getRemoteFileUrl(fileSave, false));
        }
    }

    /**
     * 通过fileId，返回图片类型文件缩略图到http返回流
     *
     * @param fileId
     * @throws IOException
     */
    public void getFilePreview(String fileId) throws IOException {
        FileSave fileSave = getById(fileId);
        if (fileSave == null) {
            return;
        }

        // 本地存储
        if (fileSave.getPlatform().startsWith("local-")) {
            // 判断大小，如果缩略图大于原图，或者没有缩略图，则直接返回原图
            String path = hasTh(fileSave) ? fileSave.getThUrl() : fileSave.getUrl();
            LocalPlusFileStorage storage = ((LocalPlusFileStorage) fileStorageService.getFileStorage("local-plus-1"));
            String fileFullPath = storage.getAbsolutePath(path);

            FaFileUtils.downloadFileShard(new File(fileFullPath), fileSave.getOriginalFilename());
        } else {
            // TO-DO 其他下载渠道直接返回URL
            // 判断大小，如果缩略图大于原图，或者没有缩略图，则直接返回原图
            HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
            if (isPrivateQiniu(fileSave)) {
                response.setHeader("Cache-Control", "private, no-store");
                response.setHeader("Referrer-Policy", "no-referrer");
            }
            response.sendRedirect(getRemoteFileUrl(fileSave, true));
        }
    }

    /** 判断是否有缩略图 */
    public boolean hasTh(FileSave fileSave) {
        if (fileSave == null) return false;
        if (StrUtil.isEmpty(fileSave.getThUrl())) return false;
        if (fileSave.getThSize() == null || fileSave.getSize() == null) return false;
        if (fileSave.getThSize() > fileSave.getSize()) return false;
        return true;
    }

    /**
     * 通过fileId读取文件的字符串内容
     *
     * @param fileId
     * @return
     */
    public String getFileStr(String fileId) {
        FileSave fileSave = getById(fileId);
        File file = getFileObj(fileSave);
        try {
            return FileUtil.readString(file, StandardCharsets.UTF_8);
        } finally {
            if (!fileSave.getPlatform().startsWith("local-")) FileUtil.del(file);
        }
    }

    /**
     * 创建一个临时文件，并入库
     *
     * @param prefix 前缀，至少3个字符
     * @param suffix 后缀，如果null则使用默认.tmp
     * @return
     */
    public FileSave createTmpFile(String prefix, String suffix) {
        File tmpFile = FileUtil.createTempFile(prefix, suffix, false);
        return upload(tmpFile);
    }

    public void removeStorage(String platform) {
        FileStorage storage = fileStorageService.getFileStorage(platform);
        if (storage != null) {
            storage.close();
            fileStorageService.getFileStorageList().remove(storage);
        }
    }

    /**
     * 系统配置更新后，同步系统配置信息
     */
    @Override
    public void syncStorageDatabaseConfig() {
        log.info("------------ Scan Database Storage Config ------------");
        FaConfig faConfig = configSysService.getConfig();
        if (faConfig.getStoreActive() == null) {
            faConfig.setStoreActive(ConfigSysStorageActiveEnum.LOCAL_PLUS);
        }
        faSetting.getFile().updateSaveType(faConfig.getStoreActive());
        //获得存储平台 List
        CopyOnWriteArrayList<FileStorage> list = fileStorageService.getFileStorageList();
        ConfigSysStorageActiveEnum storeActive = faConfig.getStoreActive();
        switch (storeActive) {
            case MINIO: {
                removeStorage(storeActive.getDesc());

                // 增加
                FileStorageProperties.MinioConfig config = new FileStorageProperties.MinioConfig();
                config.setPlatform(storeActive.getDesc());
                config.setAccessKey(faConfig.getMinioAk());
                config.setSecretKey(faConfig.getMinioSk());
                config.setEndPoint(faConfig.getMinioEndPoint());
                config.setBucketName(faConfig.getMinioBucketName());
                config.setDomain(faConfig.getMinioDomain());
                config.setBasePath(faConfig.getMinioBasePath());

                list.addAll(FileStorageServiceBuilder.buildMinioFileStorage(Collections.singletonList(config), null));
            } break;
            case QINIU: {
                removeStorage(storeActive.getDesc());

                FileStorageProperties.QiniuKodoConfig config = new FileStorageProperties.QiniuKodoConfig();
                config.setPlatform(storeActive.getDesc());
                config.setAccessKey(faConfig.getQiniuAk());
                config.setSecretKey(faConfig.getQiniuSk());
                config.setBucketName(faConfig.getQiniuBucketName());
                config.setDomain(faConfig.getQiniuDomain());
                config.setBasePath(faConfig.getQiniuBasePath());

                list.addAll(FileStorageServiceBuilder.buildQiniuKodoFileStorage(Collections.singletonList(config), null));
            } break;
            case LOCAL_PLUS: {
                LocalPlusFileStorage storage = fileStorageService.getFileStorage("local-plus-1");
                String storeLocalPath = configSysService.getStoreLocalPath();
                if (StrUtil.isNotEmpty(storeLocalPath) && !storeLocalPath.endsWith(File.separator)) {
                    storeLocalPath = storeLocalPath + File.separator;
                }
                log.info("storeLocalPath: {}", storeLocalPath);
                storage.setStoragePath(storeLocalPath);
            } break;
        }
    }

    @Override
    public File getByFileId(String fileId) {
        return getFileObj(fileId);
    }

    @Override
    public FileInfo getFileInfoById(String fileId) {
        FileSave fileSave = this.getByIdWithCache(fileId);
        return getFileInfo(fileSave);
    }

    @Override
    public void saveFileBiz(String mainBizId, String bizId, String type, String fileId) {
        fileBizBiz.saveFile(mainBizId, bizId, type, fileId);
    }

    public FileInfo getFileInfo(FileSave fileSave) {
        FileInfo fileInfo = new FileInfo();
        BeanUtil.copyProperties(fileSave, fileInfo, "attr");
        return fileInfo;
    }

    public String getFileFullPath(String fileId) {
        FileSave fileSave = getById(fileId);
        if (fileSave == null) throw new BuzzException("文件不存在或已删除");
        // 本地存储
        if (fileSave.getPlatform().startsWith("local-")) {
            LocalPlusFileStorage storage = fileStorageService.getFileStorage("local-plus-1");
            String fileFullPath = storage.getAbsolutePath(fileSave.getUrl());
            return fileFullPath;
        } else {
            return getFileUrl(null, fileSave);
        }
    }

    public String getFileUrl(String fileId) {
        String serverHost = faSetting.getUrl() == null ? null : faSetting.getUrl().getServerHost();
        return this.getFileUrl(serverHost, fileId);
    }

    public String getFileUrl(String server, String fileId) {
        return getFileUrl(server, getById(fileId));
    }

    public String getFileUrl(String server, FileSave fileSave) {
        if (fileSave == null) throw new BuzzException("文件不存在或已删除");
        // 本地存储
        if (fileSave.getPlatform().startsWith("local-")) {
            return buildLocalFileUrl(server, fileSave.getId());
        }
        return isPrivateQiniu(fileSave) ? getRemoteFileUrl(fileSave, false) : fileSave.getUrl();
    }

    private String buildLocalFileUrl(String server, String fileId) {
        String path = "/api/base/admin/fileSave/getFile/" + fileId;
        return StrUtil.isBlank(server) ? path : StrUtil.removeSuffix(server.trim(), "/") + path;
    }

}
