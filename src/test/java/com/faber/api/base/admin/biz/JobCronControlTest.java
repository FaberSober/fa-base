package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.mapper.JobMapper;
import com.faber.config.quartz.JobTask;
import com.faber.core.exception.BuzzException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobCronControlTest {
    private JobBiz biz; private JobMapper mapper; private JobTask tasks; private Job job;
    @BeforeEach void init() {
        mapper = mock(JobMapper.class); tasks = mock(JobTask.class); biz = new JobBiz();
        ReflectionTestUtils.setField(biz, "baseMapper", mapper); ReflectionTestUtils.setField(biz, "jobTask", tasks);
        job = new Job(); job.setId(10); job.setJobType(0); job.setStatus(true);
        when(mapper.selectOne(any())).thenReturn(job);
        TransactionSynchronizationManager.initSynchronization();
    }
    @AfterEach void clear() { TransactionSynchronizationManager.clearSynchronization(); }
    @Test void schedulerStartFailureDoesNotPublishEnabled() {
        assertThrows(BuzzException.class, () -> biz.startJob(10));
        verify(mapper, never()).updateById(any(Job.class));
    }
    @Test void schedulerRemovalFailureDoesNotPublishDisabled() {
        when(tasks.checkJob(job)).thenReturn(true);
        assertThrows(BuzzException.class, () -> biz.endJob(10));
        verify(mapper, never()).updateById(any(Job.class));
    }
    @Test void failedDatabaseStartIsCompensatedAfterRollback() {
        when(tasks.startJob(job)).thenReturn(true);
        assertThrows(BuzzException.class, () -> biz.startJob(10));
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(tasks).remove(job);
    }
    @Test void failedDatabasePauseRestoresRemovedScheduleAfterRollback() {
        when(tasks.checkJob(job)).thenReturn(true); when(tasks.remove(job)).thenReturn(true);
        assertThrows(BuzzException.class, () -> biz.endJob(10));
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(tasks).startJob(job);
    }
}
