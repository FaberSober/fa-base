package com.faber.api.base.admin.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.faber.api.base.admin.archive.LogArchiveDialect;
import com.faber.api.base.admin.archive.LogArchiveDialectResolver;
import com.faber.api.base.admin.archive.LogArchiveTarget;
import com.faber.api.base.admin.entity.LogArchive;
import com.faber.api.base.admin.enums.LogArchiveStatusEnum;
import com.faber.api.base.admin.mapper.LogArchiveMapper;
import com.faber.core.enums.LogArchiveRetentionPolicyEnum;
import com.faber.core.utils.FaRedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** 请求日志的按月归档编排。 */
@Slf4j
@Service
public class LogArchiveBiz {

    private static final LogArchiveTarget API_LOG_TARGET = new LogArchiveTarget("API", "base_log_api");
    private static final int DELETE_BATCH_SIZE = 1_000;
    private static final ZoneId ARCHIVE_ZONE = ZoneId.systemDefault();

    @Resource private ConfigSysBiz configSysBiz;
    @Resource private LogArchiveMapper logArchiveMapper;
    @Resource private LogArchiveDialectResolver dialectResolver;
    @Resource private DataSource dataSource;
    @Resource private FaRedisUtils faRedisUtils;

    /** 归档上一个自然月；重复执行会从已有元数据继续。 */
    public void archivePreviousMonth() {
        archivePreviousMonth(API_LOG_TARGET);
    }

    /** 为指定日志表归档上一个自然月。 */
    public void archivePreviousMonth(LogArchiveTarget target) {
        if (!Boolean.TRUE.equals(configSysBiz.getConfig().getLogArchiveEnabled())) {
            return;
        }

        RLock lock = faRedisUtils.getLock("log-archive:" + target.logType());
        boolean locked = false;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) {
                log.info("日志归档任务正在其他实例执行，跳过本次调度");
                return;
            }
            archiveMonth(target, YearMonth.now(ARCHIVE_ZONE).minusMonths(1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待日志归档锁时被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    void archiveMonth(LogArchiveTarget target, YearMonth archiveMonth) {
        String month = archiveMonth.toString();
        String archiveTable = target.archiveTable(archiveMonth);
        Timestamp startTime = timestampOf(archiveMonth.atDay(1));
        Timestamp endTime = timestampOf(archiveMonth.plusMonths(1).atDay(1));
        try (Connection connection = dataSource.getConnection()) {
            LogArchiveDialect dialect = dialectResolver.resolve(connection.getMetaData().getDatabaseProductName());
            dialect.ensureArchiveMetadataTable(connection);
        } catch (Exception e) {
            throw new IllegalStateException("初始化日志归档元数据表失败", e);
        }
        LogArchive archive = prepareArchive(target, month, archiveTable, startTime, endTime);
        if (archive == null) {
            return;
        }

        try (Connection connection = dataSource.getConnection()) {
            LogArchiveDialect dialect = dialectResolver.resolve(connection.getMetaData().getDatabaseProductName());
            updateStatus(archive, LogArchiveStatusEnum.ARCHIVING, null);
            dialect.createArchiveTable(connection, target.sourceTable(), archiveTable);

            long sourceCount = dialect.countRows(connection, target.sourceTable(), startTime, endTime);
            dialect.copyMissingRows(connection, target.sourceTable(), archiveTable, startTime, endTime);
            long archiveCount = dialect.countRows(connection, archiveTable, startTime, endTime);
            if (archiveCount < sourceCount) {
                throw new IllegalStateException("归档校验失败：源表数据量=" + sourceCount + "，归档表数据量=" + archiveCount);
            }
            deleteSourceRows(connection, dialect, target.sourceTable(), startTime, endTime);

            archive.setDataStartTime(new Date(startTime.getTime()));
            archive.setDataEndTime(new Date(endTime.getTime()));
            archive.setRowCount(sourceCount);
            archive.setArchiveTime(new Date());
            updateStatus(archive, LogArchiveStatusEnum.SUCCESS, null);
        } catch (Exception e) {
            log.error("日志归档失败，月份：{}", month, e);
            updateStatus(archive, LogArchiveStatusEnum.FAILED, rootMessage(e));
            throw new IllegalStateException("日志归档失败，月份：" + month, e);
        }
    }

    public void cleanExpiredArchives() {
        if (configSysBiz.getConfig().getLogArchiveRetentionPolicy() != LogArchiveRetentionPolicyEnum.MONTHS) {
            return;
        }
        RLock lock = faRedisUtils.getLock("log-archive-cleanup");
        boolean locked = false;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) {
                log.info("日志归档清理任务正在其他实例执行，跳过本次调度");
                return;
            }
            cleanExpiredArchivesLocked();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待日志归档清理锁时被中断", e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private void cleanExpiredArchivesLocked() {
        Integer retentionMonths = configSysBiz.getConfig().getLogArchiveRetentionMonths();
        if (retentionMonths == null || retentionMonths < 1) {
            throw new IllegalStateException("归档日志保留月数必须大于 0");
        }
        String threshold = YearMonth.now(ARCHIVE_ZONE).minusMonths(retentionMonths).toString();
        List<LogArchive> archives = logArchiveMapper.selectList(new LambdaQueryWrapper<LogArchive>()
                .eq(LogArchive::getStatus, LogArchiveStatusEnum.SUCCESS)
                .lt(LogArchive::getArchiveMonth, threshold));
        for (LogArchive archive : archives) {
            try (Connection connection = dataSource.getConnection()) {
                LogArchiveDialect dialect = dialectResolver.resolve(connection.getMetaData().getDatabaseProductName());
                dialect.dropTable(connection, archive.getArchiveTable());
                updateStatus(archive, LogArchiveStatusEnum.CLEANED, null);
            } catch (Exception e) {
                log.error("清理归档日志失败，归档表：{}", archive.getArchiveTable(), e);
                // 保持 SUCCESS，使下一次清理任务能够继续重试该已成功归档的表。
                archive.setErrorMessage(rootMessage(e));
                logArchiveMapper.updateById(archive);
            }
        }
    }

    private LogArchive prepareArchive(
            LogArchiveTarget target,
            String month,
            String archiveTable,
            Timestamp startTime,
            Timestamp endTime
    ) {
        LogArchive archive = logArchiveMapper.selectOne(new LambdaQueryWrapper<LogArchive>()
                .eq(LogArchive::getLogType, target.logType())
                .eq(LogArchive::getArchiveMonth, month));
        if (archive != null && archive.getStatus() == LogArchiveStatusEnum.SUCCESS) {
            return null;
        }
        if (archive == null) {
            archive = new LogArchive();
            archive.setLogType(target.logType());
            archive.setSourceTable(target.sourceTable());
            archive.setArchiveTable(archiveTable);
            archive.setArchiveMonth(month);
            archive.setDataStartTime(new Date(startTime.getTime()));
            archive.setDataEndTime(new Date(endTime.getTime()));
            archive.setRowCount(0L);
            archive.setStatus(LogArchiveStatusEnum.PREPARING);
            logArchiveMapper.insert(archive);
            archive = logArchiveMapper.selectOne(new LambdaQueryWrapper<LogArchive>()
                    .eq(LogArchive::getLogType, target.logType())
                    .eq(LogArchive::getArchiveMonth, month));
        } else {
            archive.setStatus(LogArchiveStatusEnum.PREPARING);
            archive.setErrorMessage(null);
            logArchiveMapper.updateById(archive);
        }
        return archive;
    }

    private void deleteSourceRows(
            Connection connection,
            LogArchiveDialect dialect,
            String sourceTable,
            Timestamp startTime,
            Timestamp endTime
    ) throws Exception {
        while (true) {
            List<Long> ids = dialect.findIds(connection, sourceTable, startTime, endTime, DELETE_BATCH_SIZE);
            if (ids.isEmpty()) {
                return;
            }
            int deletedRows = dialect.deleteByIds(connection, sourceTable, ids);
            if (deletedRows != ids.size()) {
                throw new IllegalStateException("归档源表删除校验失败，期望=" + ids.size() + "，实际=" + deletedRows);
            }
        }
    }

    private void updateStatus(LogArchive archive, LogArchiveStatusEnum status, String errorMessage) {
        archive.setStatus(status);
        archive.setErrorMessage(errorMessage);
        logArchiveMapper.updateById(archive);
    }

    private Timestamp timestampOf(LocalDate date) {
        return Timestamp.from(date.atStartOfDay(ARCHIVE_ZONE).toInstant());
    }

    private String rootMessage(Exception exception) {
        Throwable root = exception;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
    }
}
