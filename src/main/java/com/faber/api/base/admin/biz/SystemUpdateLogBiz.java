package com.faber.api.base.admin.biz;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.faber.api.base.admin.entity.SystemUpdateLog;
import com.faber.api.base.admin.mapper.SystemUpdateLogMapper;
import com.faber.api.base.admin.vo.dto.FaSqlHeader;
import com.faber.api.base.rbac.biz.RbacRoleMenuBiz;
import com.faber.core.config.dbinit.DbInit;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.utils.FaExcelUtils;
import com.faber.core.utils.FaResourceUtils;
import com.faber.core.utils.SqlUtils;
import com.faber.core.vo.msg.TableRet;
import com.faber.core.vo.query.QueryParams;
import com.faber.core.web.biz.BaseBiz;
import lombok.SneakyThrows;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BASE-系统版本更新日志表
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-08-17 17:10:02
 */
@Service
public class SystemUpdateLogBiz extends BaseBiz<SystemUpdateLogMapper, SystemUpdateLog> {

    @Resource DataSource dataSource;
    @Resource RbacRoleMenuBiz rbacRoleMenuBiz;

    public static final String SQL_SPLITTER = "-- ------------------------- info -------------------------";
    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_FAILED = 9;

    private static final long EXTENDED_LOG_VERSION = 1_000_027L;
    private static final int DB_INIT_LOCK_TIMEOUT_SECONDS = 120;
    private static final Pattern HEADER_LINE_PATTERN = Pattern.compile("^--\\s*@@(ver|info):\\s*(.+?)\\s*$");
    private static final Pattern HEADER_VERSION_PATTERN = Pattern.compile("^(\\d+)_(\\d{3})_(\\d{3})(?:L)?$");
    private static final Pattern SQL_FILE_VERSION_PATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+).*\\.sql$");
    private static final String[] LEGACY_PAGE_COLUMNS = {
            "id", "no", "name", "ver", "ver_no", "remark", "crt_time"
    };
    private static final String[] PAGE_COLUMNS = {
            "id", "no", "name", "ver", "ver_no", "remark", "crt_time",
            "status", "file_name", "checksum", "duration_ms"
    };

    @SneakyThrows
    public void initDb() {
        BaseContextHandler.useAdmin();
        boolean lockAcquired = false;
        try (Connection lockConnection = dataSource.getConnection()) {
            String databaseType = resolveDatabaseType(lockConnection.getMetaData().getDatabaseProductName());
            String lockName = buildLockName(lockConnection);
            _logger.info("数据库升级初始化使用数据库类型：{}", databaseType);
            acquireDbInitLock(lockConnection, lockName, databaseType);
            lockAcquired = true;
            try {
                List<DbInit> dbInitList = ClassUtil.scanPackageBySuper("com.faber", DbInit.class)
                        .stream()
                        .map(clazz -> (DbInit) SpringUtil.getBean(clazz))
                        .sorted(Comparator.comparing(DbInit::getOrder))
                        .toList();
                boolean extendedLogChecked = false;
                boolean extendedLogSupported = false;
                for (DbInit dbInit : dbInitList) {
                    extendedLogSupported = initOneBuzz(dbInit, databaseType, extendedLogChecked, extendedLogSupported);
                    extendedLogChecked = true;
                }

                rbacRoleMenuBiz.initAdminRoleMenu();
            } finally {
                if (lockAcquired) {
                    releaseDbInitLock(lockConnection, lockName, databaseType);
                }
            }
        } finally {
            BaseContextHandler.remove();
        }
    }

    private boolean initOneBuzz(
            DbInit dbInit,
            String databaseType,
            boolean extendedLogChecked,
            boolean extendedLogSupported
    ) throws IOException, SQLException {
        String no = dbInit.getNo();
        String name = dbInit.getName();
        List<FaSqlHeader> sqlHeaders = loadAndValidateSqlHeaders(no, databaseType);
        if (sqlHeaders.isEmpty()) {
            _logger.warn("未找到数据库升级SQL，跳过模块：{}，数据库类型：{}", no, databaseType);
            return extendedLogSupported;
        }

        try (Connection conn = dataSource.getConnection()) {
            boolean updateLogTableExists = hasSystemUpdateLogTable(conn);
            if (!extendedLogChecked) {
                extendedLogSupported = updateLogTableExists && hasExtendedLogColumns(conn);
            }
            SystemUpdateLog latestLog = updateLogTableExists ? getLatestByNo(no, extendedLogSupported) : null;

            for (FaSqlHeader header : sqlHeaders) {
                if (latestLog != null && header.getVer() <= latestLog.getVer()) {
                    continue;
                }
                executeOneSql(conn, no, name, header, extendedLogSupported);
                if (!extendedLogSupported && header.getVer() >= EXTENDED_LOG_VERSION) {
                    extendedLogSupported = hasExtendedLogColumns(conn);
                }
            }
            if (extendedLogSupported) {
                backfillLogMetadata(no, sqlHeaders);
            }
        }
        return extendedLogSupported;
    }

    private void executeOneSql(
            Connection conn,
            String no,
            String name,
            FaSqlHeader header,
            boolean extendedLogSupported
    ) {
        long startedAt = System.nanoTime();
        boolean canWriteExtendedLog = extendedLogSupported || header.getVer() >= EXTENDED_LOG_VERSION;
        try {
            _logger.info(
                    "执行升级SQL: no: {} name: {} file: {} ver: {} verNo: {}",
                    no, name, header.getFileName(), header.getVer(), header.getVerNo()
            );
            SqlUtils.executeSql(conn, header.getSql());
            long durationMs = elapsedMillis(startedAt);
            saveOrUpdateLog(no, name, header, STATUS_SUCCESS, durationMs, null, canWriteExtendedLog);
        } catch (Exception e) {
            long durationMs = elapsedMillis(startedAt);
            _logger.error("执行升级SQL失败，文件：{}", header.getFileName(), e);
            if (extendedLogSupported) {
                try {
                    saveOrUpdateLog(
                            no,
                            name,
                            header,
                            STATUS_FAILED,
                            durationMs,
                            ExceptionUtil.stacktraceToString(e),
                            true
                    );
                } catch (Exception logException) {
                    e.addSuppressed(logException);
                    _logger.error("记录升级SQL失败状态时发生异常，文件：{}", header.getFileName(), logException);
                }
            }
            throw new IllegalStateException(
                    "执行升级SQL失败，模块：" + no + "，文件：" + header.getFileName() + "，版本：" + header.getVerNo(),
                    e
            );
        }
    }

    private void saveOrUpdateLog(
            String no,
            String name,
            FaSqlHeader header,
            int status,
            long durationMs,
            String errorMsg,
            boolean extendedLogSupported
    ) {
        SystemUpdateLog updateLog = new SystemUpdateLog();
        SystemUpdateLog existingLog = baseMapper.selectPage(
                new Page<>(1, 1),
                new QueryWrapper<SystemUpdateLog>()
                        .select("id")
                        .eq("no", no)
                        .eq("ver", header.getVer())
        ).getRecords().stream().findFirst().orElse(null);
        if (existingLog != null) {
            updateLog.setId(existingLog.getId());
        }

        updateLog.setNo(no);
        updateLog.setName(name);
        updateLog.setVer(header.getVer());
        updateLog.setVerNo(header.getVerNo());
        updateLog.setRemark(header.getInfo());
        updateLog.setLog(header.getSql());
        updateLog.setCrtTime(new Date());
        if (extendedLogSupported) {
            updateLog.setStatus(status);
            updateLog.setFileName(header.getFileName());
            updateLog.setChecksum(header.getChecksum());
            updateLog.setDurationMs(durationMs);
            updateLog.setErrorMsg(errorMsg);
        }

        if (updateLog.getId() == null) {
            super.save(updateLog);
        } else {
            super.updateById(updateLog);
        }
    }

    private void backfillLogMetadata(String no, List<FaSqlHeader> sqlHeaders) {
        List<SystemUpdateLog> missingLogs = baseMapper.selectList(
                new QueryWrapper<SystemUpdateLog>()
                        .select("ver")
                        .eq("no", no)
                        .and(wrapper -> wrapper.isNull("file_name").or().isNull("checksum"))
        );
        if (missingLogs.isEmpty()) {
            return;
        }
        Map<Long, FaSqlHeader> headerByVer = new HashMap<>();
        for (FaSqlHeader header : sqlHeaders) {
            headerByVer.putIfAbsent(header.getVer(), header);
        }
        int backfilled = 0;
        for (SystemUpdateLog missingLog : missingLogs) {
            FaSqlHeader header = headerByVer.get(missingLog.getVer());
            if (header == null) {
                continue;
            }
            baseMapper.update(
                    null,
                    new UpdateWrapper<SystemUpdateLog>()
                            .eq("no", no)
                            .eq("ver", header.getVer())
                            .and(wrapper -> wrapper.isNull("file_name").or().isNull("checksum"))
                            .set("file_name", header.getFileName())
                            .set("checksum", header.getChecksum())
            );
            backfilled++;
        }
        _logger.info("回填升级日志元数据: no: {} 数量: {}", no, backfilled);
    }

    List<FaSqlHeader> loadAndValidateSqlHeaders(String no, String databaseType) throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String resourcePattern = "classpath*:sql/" + no + "/" + databaseType + "/*.sql";
        org.springframework.core.io.Resource[] resources =
                resolver.getResources(resourcePattern);

        List<FaSqlHeader> headers = new ArrayList<>(resources.length);
        Map<Long, String> versionFiles = new HashMap<>();
        for (org.springframework.core.io.Resource resource : resources) {
            FaSqlHeader header = getSqlFileHeader(resource);
            String previousFile = versionFiles.putIfAbsent(header.getVer(), header.getFileName());
            if (previousFile != null) {
                throw new IllegalStateException(
                        "SQL初始化文件版本重复，模块：" + no + "，版本：" + header.getVerNo()
                                + "，文件：" + previousFile + "、" + header.getFileName()
                );
            }
            headers.add(header);
        }
        headers.sort(Comparator.comparing(FaSqlHeader::getVer));
        return headers;
    }

    FaSqlHeader getSqlFileHeader(org.springframework.core.io.Resource resource) throws IOException {
        String sqlStr = FaResourceUtils.getResourceString(resource);
        String fileName = Objects.toString(resource.getFilename(), "<unknown>");
        if (countOccurrences(sqlStr, SQL_SPLITTER) != 2) {
            throw invalidHeader(fileName, "文件头分隔线必须且只能出现两次");
        }

        FaSqlHeader header = new FaSqlHeader();
        header.setSql(sqlStr);
        header.setFileName(fileName);
        header.setChecksum(DigestUtil.sha256Hex(sqlStr));

        int firstSplitter = sqlStr.indexOf(SQL_SPLITTER);
        int secondSplitter = sqlStr.indexOf(SQL_SPLITTER, firstSplitter + SQL_SPLITTER.length());
        String info = sqlStr.substring(firstSplitter + SQL_SPLITTER.length(), secondSplitter);
        Map<String, String> headerValues = new HashMap<>();
        for (String rawLine : info.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            Matcher lineMatcher = HEADER_LINE_PATTERN.matcher(line);
            if (!lineMatcher.matches()) {
                throw invalidHeader(fileName, "无法解析文件头行：" + line);
            }
            String previousValue = headerValues.putIfAbsent(lineMatcher.group(1), lineMatcher.group(2).trim());
            if (previousValue != null) {
                throw invalidHeader(fileName, "文件头字段重复：" + lineMatcher.group(1));
            }
        }

        String versionText = headerValues.get("ver");
        String headerInfo = headerValues.get("info");
        if (versionText == null || headerInfo == null || headerInfo.isBlank()) {
            throw invalidHeader(fileName, "文件头必须包含非空的@@ver和@@info");
        }

        Matcher versionMatcher = HEADER_VERSION_PATTERN.matcher(versionText);
        if (!versionMatcher.matches()) {
            throw invalidHeader(fileName, "@@ver格式必须为major_mmm_ppp，例如1_000_027");
        }
        int major = Integer.parseInt(versionMatcher.group(1));
        int minor = Integer.parseInt(versionMatcher.group(2));
        int patch = Integer.parseInt(versionMatcher.group(3));
        long version = major * 1_000_000L + minor * 1_000L + patch;

        Matcher fileVersionMatcher = SQL_FILE_VERSION_PATTERN.matcher(fileName);
        if (!fileVersionMatcher.matches()
                || major != Integer.parseInt(fileVersionMatcher.group(1))
                || minor != Integer.parseInt(fileVersionMatcher.group(2))
                || patch != Integer.parseInt(fileVersionMatcher.group(3))) {
            throw invalidHeader(fileName, "文件名版本与@@ver不一致");
        }

        header.setVer(version);
        header.setVerNo("V" + major + "." + minor + "." + patch);
        header.setInfo(headerInfo);
        return header;
    }

    private SystemUpdateLog getLatestByNo(String no, boolean extendedLogSupported) {
        QueryWrapper<SystemUpdateLog> wrapper = new QueryWrapper<SystemUpdateLog>()
                .select("id", "no", "ver", "ver_no")
                .eq("no", no);
        if (extendedLogSupported) {
            wrapper.eq("status", STATUS_SUCCESS);
        }
        return baseMapper.selectPage(new Page<>(1, 1), wrapper.orderByDesc("ver"))
                .getRecords()
                .stream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public TableRet<SystemUpdateLog> selectPageByQuery(QueryParams query) {
        if (query.getPageSize() > 1000) {
            throw new BuzzException("查询结果数量大于1000，请缩小查询范围");
        }
        QueryWrapper<SystemUpdateLog> wrapper = parseQuery(query);
        wrapper.select(supportsExtendedLogColumns() ? PAGE_COLUMNS : LEGACY_PAGE_COLUMNS);
        Page<SystemUpdateLog> result = super.page(
                new Page<>(query.getCurrent(), query.getPageSize()),
                wrapper
        );
        return new TableRet<>(result);
    }

    @Override
    public SystemUpdateLog getDetailById(Serializable id) {
        if (!supportsExtendedLogColumns()) {
            return super.getDetailById(id);
        }
        return baseMapper.getDetailById(Integer.parseInt(id.toString()));
    }

    @Override
    public void exportExcel(QueryParams query) throws IOException {
        QueryWrapper<SystemUpdateLog> wrapper = parseQuery(query);
        wrapper.select(supportsExtendedLogColumns() ? PAGE_COLUMNS : LEGACY_PAGE_COLUMNS);
        List<SystemUpdateLog> list = super.list(wrapper);
        FaExcelUtils.sendFileExcel(SystemUpdateLog.class, list);
    }

    private boolean supportsExtendedLogColumns() {
        try (Connection conn = dataSource.getConnection()) {
            return hasExtendedLogColumns(conn);
        } catch (SQLException e) {
            throw new IllegalStateException("检查系统版本日志表结构失败", e);
        }
    }

    private boolean hasExtendedLogColumns(Connection conn) throws SQLException {
        // 用扩展列做零行探测，避免查询系统目录。
        String sql = "SELECT status, file_name, checksum, duration_ms, error_msg FROM base_system_update_log WHERE 1 = 0";
        try (PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return true;
        } catch (SQLException e) {
            if (isMissingTableOrColumnError(e)) {
                return false;
            }
            throw e;
        }
    }

    private boolean hasSystemUpdateLogTable(Connection conn) throws SQLException {
        String sql = "SELECT 1 FROM base_system_update_log WHERE 1 = 0";
        try (PreparedStatement statement = conn.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return true;
        } catch (SQLException e) {
            if (isMissingTableOrColumnError(e)) {
                return false;
            }
            throw e;
        }
    }

    private boolean isMissingTableOrColumnError(SQLException e) {
        // MySQL: 1054未知列(42S22)、1146表不存在(42S02)
        // PostgreSQL: 42703列不存在、42P01表不存在；Oracle: ORA-00904、ORA-00942
        return e.getErrorCode() == 1054 || e.getErrorCode() == 1146
                || e.getErrorCode() == 904 || e.getErrorCode() == 942
                || "42S22".equals(e.getSQLState()) || "42S02".equals(e.getSQLState())
                || "42703".equals(e.getSQLState()) || "42P01".equals(e.getSQLState());
    }

    private void acquireDbInitLock(Connection conn, String lockName, String databaseType) throws SQLException {
        if ("mysql".equals(databaseType)) {
            acquireMysqlDbInitLock(conn, lockName);
            return;
        }
        if ("postgre".equals(databaseType)) {
            acquirePostgreDbInitLock(conn, lockName);
            return;
        }
        acquireOracleDbInitLock(conn, lockName);
    }

    private void acquireMysqlDbInitLock(Connection conn, String lockName) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, lockName);
            statement.setInt(2, DB_INIT_LOCK_TIMEOUT_SECONDS);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new IllegalStateException("获取数据库初始化锁失败：" + lockName);
                }
            }
        }
    }

    private void acquirePostgreDbInitLock(Connection conn, String lockName) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement("SELECT pg_advisory_lock(?)")) {
            statement.setQueryTimeout(DB_INIT_LOCK_TIMEOUT_SECONDS);
            statement.setLong(1, toPostgreLockId(lockName));
            statement.executeQuery();
        }
    }

    private void acquireOracleDbInitLock(Connection conn, String lockName) throws SQLException {
        try (PreparedStatement statement = conn.prepareStatement("SELECT DBMS_LOCK.REQUEST(?, 6, ?) FROM DUAL")) {
            statement.setInt(1, toOracleLockId(lockName));
            statement.setInt(2, DB_INIT_LOCK_TIMEOUT_SECONDS);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 0) {
                    throw new IllegalStateException("获取 Oracle 数据库初始化锁失败：" + lockName);
                }
            }
        }
    }

    private void releaseDbInitLock(Connection conn, String lockName, String databaseType) {
        if ("mysql".equals(databaseType)) {
            releaseMysqlDbInitLock(conn, lockName);
            return;
        }
        if ("postgre".equals(databaseType)) {
            releasePostgreDbInitLock(conn, lockName);
            return;
        }
        releaseOracleDbInitLock(conn, lockName);
    }

    private void releaseMysqlDbInitLock(Connection conn, String lockName) {
        try (PreparedStatement statement = conn.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, lockName);
            statement.executeQuery();
        } catch (SQLException e) {
            _logger.warn("释放数据库初始化锁失败：{}", lockName, e);
        }
    }

    private void releasePostgreDbInitLock(Connection conn, String lockName) {
        try (PreparedStatement statement = conn.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            statement.setLong(1, toPostgreLockId(lockName));
            statement.executeQuery();
        } catch (SQLException e) {
            _logger.warn("释放数据库初始化锁失败：{}", lockName, e);
        }
    }

    private void releaseOracleDbInitLock(Connection conn, String lockName) {
        try (PreparedStatement statement = conn.prepareStatement("SELECT DBMS_LOCK.RELEASE(?) FROM DUAL")) {
            statement.setInt(1, toOracleLockId(lockName));
            statement.executeQuery();
        } catch (SQLException e) {
            _logger.warn("释放 Oracle 数据库初始化锁失败：{}", lockName, e);
        }
    }

    private String buildLockName(Connection conn) throws SQLException {
        String lockName = "fa-admin:db-init:" + Objects.toString(conn.getCatalog(), "default");
        return lockName.length() <= 64 ? lockName : lockName.substring(0, 64);
    }

    static String resolveDatabaseType(String databaseProductName) {
        String normalizedName = Objects.toString(databaseProductName, "").toLowerCase(Locale.ROOT);
        if (normalizedName.contains("mysql")) {
            return "mysql";
        }
        if (normalizedName.contains("postgresql")) {
            return "postgre";
        }
        if (normalizedName.contains("oracle")) {
            return "oracle";
        }
        throw new IllegalStateException("不支持的数据库类型：" + Objects.toString(databaseProductName, "<unknown>")
                + "，仅支持 MySQL、PostgreSQL、Oracle");
    }

    private long toPostgreLockId(String lockName) {
        return Long.parseUnsignedLong(DigestUtil.sha256Hex(lockName).substring(0, 16), 16);
    }

    private int toOracleLockId(String lockName) {
        return (int) (Math.floorMod(toPostgreLockId(lockName), 1_073_741_823L) + 1);
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000L;
    }

    private int countOccurrences(String text, String target) {
        int count = 0;
        int fromIndex = 0;
        while ((fromIndex = text.indexOf(target, fromIndex)) >= 0) {
            count++;
            fromIndex += target.length();
        }
        return count;
    }

    private IllegalStateException invalidHeader(String fileName, String reason) {
        return new IllegalStateException("SQL初始化文件头错误，文件：" + fileName + "，原因：" + reason);
    }

}
