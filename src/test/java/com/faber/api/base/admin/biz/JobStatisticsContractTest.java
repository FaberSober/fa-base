package com.faber.api.base.admin.biz;

import com.alibaba.excel.EasyExcel;
import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.enums.JobLogStatusEnum;
import com.faber.core.config.easyexcel.MyEasyExcelConverterLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JobStatisticsContractTest {
    @Test
    void statisticsAreReadableButCannotBeSubmittedThroughTheOrdinaryJsonContract() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Job submitted = mapper.readValue("""
                {"jobName":"允许编辑名称","executeCount":999,"successCount":999,
                 "failCount":999,"lastExecuteTime":1,"lastJobLogId":999,
                 "lastStatus":9,"totalDurationMs":999,"durationSampleCount":999,
                 "avgDurationMs":999,"statsSince":1}
                """, Job.class);
        assertEquals("允许编辑名称", submitted.getJobName());
        assertEquals(0L, submitted.getExecuteCount());
        assertEquals(0L, submitted.getSuccessCount());
        assertEquals(0L, submitted.getTotalDurationMs());
        assertNull(submitted.getLastJobLogId());
        assertNull(submitted.getLastExecuteTime());
        assertNull(submitted.getLastStatus());
        assertNull(submitted.getStatsSince());
        assertNull(submitted.getAvgDurationMs());

        submitted.setTotalDurationMs(1001L);
        submitted.setDurationSampleCount(2L);
        submitted.setLastStatus(JobLogStatusEnum.DONE);
        var json = mapper.valueToTree(submitted);
        assertEquals(501L, json.get("avgDurationMs").longValue());
        assertEquals(2, json.get("lastStatus").intValue());
    }

    @Test
    void excelUsesTheSameComputedAverageAndReadableExecutionResult() {
        new MyEasyExcelConverterLoader().defaultConverterLoader();
        Job job = new Job();
        job.setJobName("统计导出");
        job.setTotalDurationMs(1001L);
        job.setDurationSampleCount(2L);
        job.setLastStatus(JobLogStatusEnum.DONE);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        EasyExcel.write(output, Job.class).sheet().doWrite(List.of(job));
        List<Map<Integer, String>> rows = EasyExcel.read(new ByteArrayInputStream(output.toByteArray()))
                .headRowNumber(0).sheet().doReadSync();
        assertEquals(2, rows.size());
        int average = column(rows.get(0), "平均总耗时(ms)");
        assertNotNull(rows.get(1).get(column(rows.get(0), "累计总耗时(ms)")));
        assertNotNull(rows.get(1).get(column(rows.get(0), "参与平均的执行次数")));
        column(rows.get(0), "耗时统计起始时间");
        int result = column(rows.get(0), "最近执行结果");
        assertEquals(501L, new BigDecimal(rows.get(1).get(average)).longValueExact());
        assertEquals("成功", rows.get(1).get(result));
    }

    private int column(Map<Integer, String> header, String title) {
        return header.entrySet().stream().filter(entry -> title.equals(entry.getValue()))
                .map(Map.Entry::getKey).findFirst().orElseThrow();
    }
}
