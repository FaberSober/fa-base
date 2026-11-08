package com.faber.api.base.admin.biz;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.api.base.admin.mapper.JobLogMapper;
import com.faber.api.base.admin.mapper.JobMapper;
import com.faber.api.base.admin.mapper.JobFlowNodeRunMapper;
import com.faber.core.config.mybatis.handler.MysqlMetaObjectHandler;
import com.faber.core.constant.FaSetting;
import com.faber.core.context.BaseContextHandler;
import org.apache.ibatis.type.JdbcType;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real Mapper and Spring transaction regression checks; no application/server startup.
 * H2 verifies persistence behavior, not target MySQL/PostgreSQL migration acceptance.
 */
class JobExecutionPersistenceTest {

    private static final long START = Instant.parse("2026-11-08T02:00:00Z").toEpochMilli();

    private JdbcTemplate jdbc;
    private JobMapper jobs;
    private JobLogMapper logs;
    private JobLogBiz biz;

    @BeforeEach
    void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:job_execution_" + UUID.randomUUID()
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        jdbc = new JdbcTemplate(dataSource);
        createSchema();
        BaseContextHandler.useAdmin();

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setDatabaseId("postgre");
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setJdbcTypeForNull(JdbcType.NULL);
        configuration.setDefaultEnumTypeHandler(CompositeEnumTypeHandler.class);

        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setBanner(false);
        globalConfig.setDbConfig(new GlobalConfig.DbConfig()
                .setLogicNotDeleteValue("false").setLogicDeleteValue("true"));
        globalConfig.setMetaObjectHandler(new MysqlMetaObjectHandler(new FaSetting()));

        MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
        factory.setDataSource(dataSource);
        factory.setConfiguration(configuration);
        factory.setGlobalConfig(globalConfig);
        factory.setMapperLocations(new ClassPathResource("mapper/base/admin/JobMapper.xml"),
                new ClassPathResource("mapper/base/admin/JobLogMapper.xml"));
        SqlSessionTemplate sessions = new SqlSessionTemplate(Objects.requireNonNull(factory.getObject()));
        sessions.getConfiguration().addMapper(JobFlowNodeRunMapper.class);
        jobs = sessions.getMapper(JobMapper.class);
        logs = sessions.getMapper(JobLogMapper.class);

        JobLogBiz target = new JobLogBiz();
        ReflectionTestUtils.setField(target, "baseMapper", logs);
        ReflectionTestUtils.setField(target, "jobMapper", jobs);
        ReflectionTestUtils.setField(target, "flowNodeRuns", sessions.getMapper(JobFlowNodeRunMapper.class));
        ProxyFactory proxy = new ProxyFactory(target);
        proxy.setProxyTargetClass(true);
        proxy.addAdvice(new TransactionInterceptor(new DataSourceTransactionManager(dataSource),
                new AnnotationTransactionAttributeSource()));
        biz = (JobLogBiz) proxy.getProxy();
    }

    @AfterEach
    void tearDown() {
        BaseContextHandler.remove();
        if (jdbc != null) jdbc.execute("SHUTDOWN");
    }

    @Test
    void persistsStartDispatchAndFinalResultExactlyOnce() {
        Job job = createJob("lifecycle");
        JobLog record = start(job, START);
        Job active = jobs.selectById(job.getId());
        assertStats(active, 1, 0, 0, 0, 0);
        assertEquals(record.getId(), active.getLastJobLogId());
        assertEquals(new Date(START), active.getLastExecuteTime());
        assertEquals(JobLogStatusEnum.DOING, active.getLastStatus());
        assertNull(active.getLastEndTime());
        assertNull(active.getAvgDurationMs());

        record.setDispatchEndTime(new Date(START + 20));
        record.setDispatchDurationMs(20L);
        biz.recordDispatch(record);
        record.setDispatchEndTime(new Date(START + 80));
        record.setDispatchDurationMs(80L);
        biz.recordDispatch(record);
        assertEquals(20L, logs.selectById(record.getId()).getDispatchDurationMs());
        assertEquals(20L, jobs.selectById(job.getId()).getLastDispatchDurationMs());

        complete(record, JobLogStatusEnum.DONE, 1500L, 20L);
        assertTrue(biz.finishExecution(record));
        assertFalse(biz.finishExecution(record));
        Job finished = jobs.selectById(job.getId());
        assertStats(finished, 1, 1, 0, 1500, 1);
        assertEquals(1500L, finished.getAvgDurationMs());
        assertEquals(JobLogStatusEnum.DONE, finished.getLastStatus());
        assertEquals(new Date(START + 1500), finished.getLastEndTime());
        assertEquals(1500L, finished.getLastDurationMs());
        assertEquals(1L, logs.selectById(record.getId()).getDuration());
    }

    @Test
    void olderRoundFinishingLateOnlyAddsItsOwnStats() {
        Job job = createJob("overlapping");
        JobLog older = start(job, START);
        JobLog newer = start(job, START + 100);
        complete(newer, JobLogStatusEnum.DONE, 400L, 10L);
        assertTrue(biz.finishExecution(newer));

        older.setDispatchEndTime(new Date(START + 900));
        older.setDispatchDurationMs(900L);
        biz.recordDispatch(older);
        complete(older, JobLogStatusEnum.ERROR, 1200L, 900L);
        older.setErrMsg("older round failed");
        assertTrue(biz.finishExecution(older));

        Job snapshot = jobs.selectById(job.getId());
        assertStats(snapshot, 2, 1, 1, 1600, 2);
        assertEquals(800L, snapshot.getAvgDurationMs());
        assertEquals(newer.getId(), snapshot.getLastJobLogId());
        assertEquals(newer.getBeginTime(), snapshot.getLastExecuteTime());
        assertEquals(newer.getEndTime(), snapshot.getLastEndTime());
        assertEquals(JobLogStatusEnum.DONE, snapshot.getLastStatus());
        assertEquals(400L, snapshot.getLastDurationMs());
        assertEquals(10L, snapshot.getLastDispatchDurationMs());
    }

    @Test
    void startOrderingPreservesMillisecondsAndUsesIdOnlyForEqualStartTimes() {
        Job job = createJob("start ordering");
        JobLog newer = start(job, START + 901);
        JobLog delayedOlder = start(job, START + 900);
        assertTrue(delayedOlder.getId() > newer.getId());
        assertEquals(newer.getId(), jobs.selectById(job.getId()).getLastJobLogId());
        assertEquals(new Date(START + 901), jobs.selectById(job.getId()).getLastExecuteTime());

        complete(newer, JobLogStatusEnum.DONE, 100L, 5L);
        assertTrue(biz.finishExecution(newer));
        JobLog sameMillisecond = start(job, START + 901);
        assertTrue(sameMillisecond.getId() > newer.getId());
        Job snapshot = jobs.selectById(job.getId());
        assertEquals(sameMillisecond.getId(), snapshot.getLastJobLogId());
        assertEquals(JobLogStatusEnum.DOING, snapshot.getLastStatus());
        assertNull(snapshot.getLastEndTime());
        assertNull(snapshot.getLastDurationMs());
        assertNull(snapshot.getLastDispatchDurationMs());
        assertEquals(3L, snapshot.getExecuteCount());
    }

    @Test
    void genericEntityInsertAndStaleUpdateCannotWriteServerOwnedStatistics() {
        Job job = createJob("stale form");
        Job stale = jobs.selectById(job.getId());
        JobLog record = start(job, START);
        complete(record, JobLogStatusEnum.DONE, 1700L, 30L);
        assertTrue(biz.finishExecution(record));
        Job expected = jobs.selectById(job.getId());

        poisonStatistics(stale);
        stale.setJobName("ordinary edit still works");
        assertEquals(1, jobs.updateById(stale));
        Job edited = jobs.selectById(job.getId());
        assertEquals("ordinary edit still works", edited.getJobName());
        assertStatisticsEqual(expected, edited);

        Job forged = job("forged create");
        poisonStatistics(forged);
        assertEquals(1, jobs.insert(forged));
        Job created = jobs.selectById(forged.getId());
        assertStats(created, 0, 0, 0, 0, 0);
        assertNull(created.getLastJobLogId());
        assertNull(created.getLastExecuteTime());
        assertNull(created.getLastEndTime());
        assertNull(created.getLastStatus());
        assertNull(created.getLastDurationMs());
        assertNull(created.getLastDispatchDurationMs());
        assertNull(created.getLastTrackingVersion());
        assertNull(created.getStatsSince());
        assertNull(created.getAvgDurationMs());
    }

    @Test
    void oldCountersAndUnconfirmedResultsAreExcludedFromNewDurationSamples() {
        Job job = createJob("new average");
        jdbc.update("UPDATE base_job SET execute_count=10, success_count=8, fail_count=2,"
                + " stats_since=? WHERE id=?", new Date(START - 1000), job.getId());

        JobLog unconfirmed = start(job, START);
        unconfirmed.setTrackingVersion(2);
        complete(unconfirmed, JobLogStatusEnum.UNCONFIRMED, null, 5L);
        assertTrue(biz.finishExecution(unconfirmed));
        assertFalse(biz.finishExecution(unconfirmed));
        Job pending = jobs.selectById(job.getId());
        assertStats(pending, 11, 8, 2, 0, 0);
        assertNull(pending.getAvgDurationMs());
        assertEquals(JobLogStatusEnum.UNCONFIRMED, pending.getLastStatus());
        assertEquals(2, pending.getLastTrackingVersion());
        assertNull(pending.getLastEndTime());
        assertNull(pending.getLastDurationMs());

        JobLog success = start(job, START + 1000);
        complete(success, JobLogStatusEnum.DONE, 400L, 5L);
        assertTrue(biz.finishExecution(success));
        JobLog failure = start(job, START + 2000);
        complete(failure, JobLogStatusEnum.ERROR, 601L, 5L);
        assertTrue(biz.finishExecution(failure));
        Job snapshot = jobs.selectById(job.getId());
        assertStats(snapshot, 13, 9, 3, 1001, 2);
        assertEquals(501L, snapshot.getAvgDurationMs());
        assertEquals(new Date(START - 1000), snapshot.getStatsSince());
    }

    @Test
    void failureUpdatingTotalsRollsBackFinalStatusAndAllowsOneRetry() {
        Job job = createJob("rollback finish");
        JobLog record = start(job, START);
        jdbc.execute("ALTER TABLE base_job ADD CONSTRAINT reject_new_duration CHECK (total_duration_ms = 0)");
        complete(record, JobLogStatusEnum.DONE, 500L, 10L);

        assertThrows(RuntimeException.class, () -> biz.finishExecution(record));
        JobLog stillRunning = logs.selectById(record.getId());
        assertEquals(JobLogStatusEnum.DOING, stillRunning.getStatus());
        assertNull(stillRunning.getEndTime());
        assertNull(stillRunning.getDurationMs());
        Job unchanged = jobs.selectById(job.getId());
        assertStats(unchanged, 1, 0, 0, 0, 0);
        assertEquals(JobLogStatusEnum.DOING, unchanged.getLastStatus());
        assertNull(unchanged.getLastEndTime());

        jdbc.execute("ALTER TABLE base_job DROP CONSTRAINT reject_new_duration");
        assertTrue(biz.finishExecution(record));
        assertFalse(biz.finishExecution(record));
        assertStats(jobs.selectById(job.getId()), 1, 1, 0, 500, 1);
    }

    @Test
    void missingJobRollsBackTheAlreadyInsertedExecutionRecord() {
        JobLog record = new JobLog();
        record.setJobId(404);
        record.setBeginTime(new Date(START));
        record.setStatus(JobLogStatusEnum.DOING);
        record.setDuration(0L);
        record.setTrackingVersion(1);
        assertThrows(IllegalStateException.class, () -> biz.beginExecution(record));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM base_job_log", Integer.class));
    }

    private Job createJob(String name) {
        Job job = job(name);
        assertEquals(1, jobs.insert(job));
        return job;
    }

    private Job job(String name) {
        Job job = new Job();
        job.setJobName(name);
        job.setCron("0 * * * * ?");
        job.setClazzPath("test.Job");
        return job;
    }

    private JobLog start(Job job, long beginTime) {
        JobLog record = new JobLog();
        record.setJobId(job.getId());
        record.setBeginTime(new Date(beginTime));
        record.setStatus(JobLogStatusEnum.DOING);
        record.setDuration(0L);
        record.setTrackingVersion(1);
        biz.beginExecution(record);
        assertNotNull(record.getId());
        return record;
    }

    private void complete(JobLog record, JobLogStatusEnum status, Long durationMs, Long dispatchMs) {
        record.setStatus(status);
        record.setDurationMs(durationMs);
        record.setDuration(durationMs == null ? 0L : durationMs / 1000);
        record.setEndTime(durationMs == null ? null : new Date(record.getBeginTime().getTime() + durationMs));
        record.setDispatchDurationMs(dispatchMs);
        record.setDispatchEndTime(new Date(record.getBeginTime().getTime() + dispatchMs));
    }

    private void assertStats(Job job, long execute, long success, long failure, long total, long samples) {
        assertEquals(execute, job.getExecuteCount());
        assertEquals(success, job.getSuccessCount());
        assertEquals(failure, job.getFailCount());
        assertEquals(total, job.getTotalDurationMs());
        assertEquals(samples, job.getDurationSampleCount());
    }

    private void poisonStatistics(Job job) {
        job.setLastJobLogId(999);
        job.setLastExecuteTime(new Date(1));
        job.setLastEndTime(new Date(2));
        job.setLastStatus(JobLogStatusEnum.ERROR);
        job.setLastDurationMs(999L);
        job.setLastDispatchDurationMs(999L);
        job.setLastTrackingVersion(2);
        job.setExecuteCount(999L);
        job.setSuccessCount(999L);
        job.setFailCount(999L);
        job.setTotalDurationMs(999L);
        job.setDurationSampleCount(999L);
        job.setStatsSince(new Date(3));
    }

    private void assertStatisticsEqual(Job expected, Job actual) {
        for (String field : List.of("lastJobLogId", "lastExecuteTime", "lastEndTime", "lastStatus",
                "lastDurationMs", "lastDispatchDurationMs", "lastTrackingVersion", "executeCount",
                "successCount", "failCount", "totalDurationMs", "durationSampleCount", "statsSince")) {
            assertEquals(ReflectionTestUtils.getField(expected, field), ReflectionTestUtils.getField(actual, field), field);
        }
        assertEquals(expected.getAvgDurationMs(), actual.getAvgDurationMs());
    }

    @Test
    void classOnlyNodePersistsWithoutCreatingOrUpdatingTaskConfiguration() {
        JobLog record = new JobLog(); record.setJobType(0); record.setTriggerSource(3);
        record.setClazzPath("com.example.Node"); record.setJobName("节点");
        record.setBeginTime(new Date(START)); record.setStatus(JobLogStatusEnum.DOING);
        record.setDuration(0L); record.setTrackingVersion(1);
        biz.beginExecution(record);
        assertNull(logs.selectById(record.getId()).getJobId());
        complete(record, JobLogStatusEnum.DONE, 100L, 10L);
        assertTrue(biz.finishExecution(record));
        assertFalse(biz.finishExecution(record));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM base_job", Integer.class));
    }

    private void createSchema() {
        jdbc.execute("""
            CREATE TABLE base_job_flow_node_run (
              id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
              flow_log_id INTEGER, node_id VARCHAR(100), clazz_path VARCHAR(255), node_name VARCHAR(255),
              attempt INTEGER DEFAULT 1, job_log_id INTEGER UNIQUE, status INTEGER DEFAULT 0,
              begin_time TIMESTAMP(3), end_time TIMESTAMP(3), reason TEXT, crt_time TIMESTAMP(3), upd_time TIMESTAMP(3),
              UNIQUE(flow_log_id, node_id, attempt)
            )
            """);
        jdbc.execute("""
                CREATE TABLE base_job (
                    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    job_name VARCHAR(255), cron VARCHAR(255), status BOOLEAN NOT NULL DEFAULT false,
                    job_type INTEGER NOT NULL DEFAULT 0, flow_definition TEXT,
                    clazz_path VARCHAR(255), job_desc VARCHAR(255), detail_log_enabled BOOLEAN DEFAULT false,
                    crt_time TIMESTAMP(3), crt_user VARCHAR(32), crt_name VARCHAR(255), crt_host VARCHAR(255),
                    upd_time TIMESTAMP(3), upd_user VARCHAR(32), upd_name VARCHAR(255), upd_host VARCHAR(255),
                    deleted BOOLEAN NOT NULL DEFAULT false,
                    last_job_log_id INTEGER, last_execute_time TIMESTAMP(3), last_end_time TIMESTAMP(3),
                    last_status SMALLINT, last_duration_ms BIGINT, last_dispatch_duration_ms BIGINT,
                    last_tracking_version INTEGER, execute_count BIGINT NOT NULL DEFAULT 0,
                    success_count BIGINT NOT NULL DEFAULT 0, fail_count BIGINT NOT NULL DEFAULT 0,
                    total_duration_ms BIGINT NOT NULL DEFAULT 0, duration_sample_count BIGINT NOT NULL DEFAULT 0,
                    stats_since TIMESTAMP(3)
                )
                """);
        jdbc.execute("""
                CREATE TABLE base_job_log (
                    id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                    job_type INTEGER NOT NULL DEFAULT 0, trigger_source INTEGER NOT NULL DEFAULT 0,
                    flow_snapshot TEXT, clazz_path VARCHAR(255), job_name VARCHAR(255),
                    job_id INTEGER, begin_time TIMESTAMP(3) NOT NULL, end_time TIMESTAMP(3),
                    status SMALLINT NOT NULL, duration BIGINT NOT NULL DEFAULT 0,
                    err_msg VARCHAR(2048), log_path VARCHAR(255), dispatch_end_time TIMESTAMP(3),
                    dispatch_duration_ms BIGINT, duration_ms BIGINT, tracking_version INTEGER NOT NULL DEFAULT 0
                )
                """);
    }
}
