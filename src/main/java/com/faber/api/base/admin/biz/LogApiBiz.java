package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.LogApi;
import com.faber.api.base.admin.entity.LogArchive;
import com.faber.api.base.admin.enums.LogArchiveStatusEnum;
import com.faber.api.base.admin.mapper.LogApiMapper;
import com.faber.api.base.admin.mapper.LogArchiveMapper;
import com.faber.core.utils.FaFileUtils;
import com.faber.core.exception.BuzzException;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.vo.tree.TreeNode;
import com.faber.core.web.biz.BaseBiz;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * URL请求日志
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class LogApiBiz extends BaseBiz<LogApiMapper, LogApi> {

    private static final String API_LOG_TYPE = "API";
    private static final Pattern ARCHIVE_TABLE_PATTERN = Pattern.compile("^base_log_api_[0-9]{4}_[0-9]{2}$");
    private static final int MAX_QUERY_MONTHS = 12;
    private static final ZoneId ARCHIVE_ZONE = ZoneId.systemDefault();

    @Resource private LogArchiveMapper logArchiveMapper;

    public void deleteAll() {
        baseMapper.deleteAll();
    }

    @Override
    public TableRet<LogApi> selectPageByQuery(QueryParams query) {
        if (query.getPageSize() > 1000) {
            throw new BuzzException("查询结果数量大于1000，请缩小查询范围");
        }
        List<String> targetTables = resolveQueryTables(query);
        QueryWrapper<LogApi> wrapper = parseQuery(query);
        if (query.getSorterInfo().isEmpty()) {
            wrapper.orderByDesc("crt_time");
        }
        Page<LogApi> page = new Page<>(query.getCurrent(), query.getPageSize());
        TableRet<LogApi> result = new TableRet<>(baseMapper.selectPageFromTables(page, targetTables, wrapper));
        addEnumOptions(result, LogApi.class);
        return result;
    }

    private List<String> resolveQueryTables(QueryParams query) {
        Date startTime = queryTime(query, "crtTime#$min");
        Date endTime = queryTime(query, "crtTime#$max");
        if (startTime == null && endTime == null) {
            return List.of("base_log_api");
        }
        if (startTime == null || endTime == null || startTime.after(endTime)) {
            throw new BuzzException("历史日志查询必须提供完整且合法的请求时间范围");
        }

        YearMonth startMonth = YearMonth.from(startTime.toInstant().atZone(ARCHIVE_ZONE));
        YearMonth endMonth = YearMonth.from(endTime.toInstant().atZone(ARCHIVE_ZONE));
        long months = ChronoUnit.MONTHS.between(startMonth, endMonth) + 1;
        if (months > MAX_QUERY_MONTHS) {
            throw new BuzzException("单次日志查询最多跨 " + MAX_QUERY_MONTHS + " 个月，请缩小请求时间范围");
        }

        List<String> tables = new ArrayList<>();
        YearMonth currentMonth = YearMonth.now(ARCHIVE_ZONE);
        if (!startMonth.isAfter(currentMonth) && !endMonth.isBefore(currentMonth)) {
            tables.add("base_log_api");
        }
        List<LogArchive> archives = logArchiveMapper.selectList(new QueryWrapper<LogArchive>()
                .eq("log_type", API_LOG_TYPE)
                .eq("status", LogArchiveStatusEnum.SUCCESS)
                .between("archive_month", startMonth.toString(), endMonth.toString())
                .orderByAsc("archive_month"));
        for (LogArchive archive : archives) {
            if (ARCHIVE_TABLE_PATTERN.matcher(archive.getArchiveTable()).matches()) {
                tables.add(archive.getArchiveTable());
            }
        }
        if (tables.isEmpty()) {
            // SQL UNION 不能接收空表集合；不存在归档记录时返回空的当前表范围查询。
            tables.add("base_log_api");
        }
        return tables;
    }

    private Date queryTime(QueryParams query, String key) {
        Object value = query.getQuery().get(key);
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        try {
            return cn.hutool.core.date.DateUtil.parse(value.toString()).toJdkDate();
        } catch (Exception e) {
            throw new BuzzException("请求时间格式错误：" + value);
        }
    }

    public List<TreeNode<Object>> listLogFiles() {
        File logDir = new File("./log");
        if (!logDir.exists() || !logDir.isDirectory()) {
            return new ArrayList<>();
        }
        return listFilesRecursive(logDir, "");
    }

    private List<TreeNode<Object>> listFilesRecursive(File dir, String path) {
        List<TreeNode<Object>> nodes = new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return nodes;

        // Sort: directories first, then files by name
        Arrays.sort(files, Comparator.comparing(File::isDirectory).reversed().thenComparing(File::getName));

        for (File file : files) {
            String currentPath = path.isEmpty() ? file.getName() : path + "/" + file.getName();
            TreeNode<Object> node = new TreeNode<>();
            node.setId(currentPath);
            node.setName(file.getName());
            
            Map<String, Object> sourceData = new HashMap<>();
            sourceData.put("isDir", file.isDirectory());
            sourceData.put("size", file.length());
            sourceData.put("sizeStr", formatFileSize(file.length()));
            node.setSourceData(sourceData);

            if (file.isDirectory()) {
                node.setChildren(listFilesRecursive(file, currentPath));
                node.setHasChildren(!node.getChildren().isEmpty());
            } else {
                node.setHasChildren(false);
            }
            nodes.add(node);
        }
        return nodes;
    }

    public List<String> readLogFile(String filePath, int lines) {
        File file = new File("./log", filePath);
        if (!file.exists() || !file.isFile()) {
            throw new RuntimeException("Log file not found: " + filePath);
        }

        List<String> result = new ArrayList<>();
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long length = raf.length();
            if (length == 0) return result;

            long pos = length - 1;
            int count = 0;
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

            while (pos >= 0 && count < lines) {
                raf.seek(pos);
                int b = raf.read();
                if (b == '\n') {
                    if (baos.size() > 0) {
                        byte[] bytes = baos.toByteArray();
                        reverseBytes(bytes);
                        result.add(0, new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                        baos.reset();
                        count++;
                    }
                } else if (b != '\r') {
                    baos.write(b);
                }
                pos--;
            }
            if (baos.size() > 0 && count < lines) {
                byte[] bytes = baos.toByteArray();
                reverseBytes(bytes);
                result.add(0, new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading log file", e);
        }
        return result;
    }

    private void reverseBytes(byte[] array) {
        int i = 0;
        int j = array.length - 1;
        while (i < j) {
            byte temp = array[i];
            array[i] = array[j];
            array[j] = temp;
            i++;
            j--;
        }
    }

    public void downloadLogFile(String filePath) {
        if (filePath == null || filePath.contains("..") || filePath.startsWith("/")) {
            throw new RuntimeException("Invalid file path: " + filePath);
        }
        File file = new File("./log", filePath);
        if (!file.exists() || !file.isFile()) {
            throw new RuntimeException("Log file not found: " + filePath);
        }

        try {
            FaFileUtils.downloadFile(file);
        } catch (IOException e) {
            throw new RuntimeException("Error downloading log file", e);
        }
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new java.text.DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

}
