package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.mapper.JobLogMapper;
import com.faber.api.base.admin.mapper.JobMapper;
import com.faber.api.base.admin.vo.ret.JobVisualPlan;
import com.faber.core.exception.BuzzException;
import org.quartz.CronExpression;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class JobVisualPlanService {
    private final JobMapper jobs;
    private final JobLogMapper logs;
    public JobVisualPlanService(JobMapper jobs, JobLogMapper logs) { this.jobs = jobs; this.logs = logs; }

    private LocalDate parseDate(String date) {
        try {
            if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException();
            return LocalDate.parse(date);
        } catch (DateTimeException | IllegalArgumentException e) { throw new BuzzException("日期格式应为 yyyy-MM-dd"); }
    }

    public JobVisualPlan plan(String date, Integer type, String keyword) {
        var day = parseDate(date);
        if (type != null && type != 0 && type != 1) throw new BuzzException("任务类型无效");
        var now = Instant.now(); var zone = ZoneId.systemDefault();
        var result = new ArrayList<JobVisualPlan.Item>();
        for (var job : jobs.selectList(null)) {
            if (!Boolean.TRUE.equals(job.getStatus())) continue;
            if (type != null && !Objects.equals(type, job.getJobType() == null ? 0 : job.getJobType())) continue;
            if (keyword != null && !keyword.isBlank() && !Objects.toString(job.getJobName(), "").contains(keyword.trim())) continue;
            var points = points(job, day, now, zone);
            if (points.isEmpty()) continue;
            Long duration = logs.recentSuccessAverage(job.getId());
            int[] counts = new int[96];
            for (int second : points) counts[Math.min(95, second / 900)]++;
            if (points.size() > 2000) {
                for (int i = 0; i < counts.length; i++) if (counts[i] > 0)
                    result.add(item(job, i * 900, (i + 1) * 900, duration, counts[i]));
            } else for (int second : points) result.add(item(job, second, second, duration, 1));
        }
        return new JobVisualPlan(result);
    }

    private static JobVisualPlan.Item item(Job job, int start, int end, Long duration, int count) {
        return new JobVisualPlan.Item(job.getId(), job.getJobName(), job.getJobType(), job.getCron(), !Boolean.TRUE.equals(job.getStatus()), start, end, duration, count);
    }

    static List<Integer> points(Job job, LocalDate day, Instant now, ZoneId zone) {
        var result = new ArrayList<Integer>();
        var start = day.atStartOfDay(zone).toInstant(); var end = day.plusDays(1).atStartOfDay(zone).toInstant();
        if (!end.isAfter(now) || job.getCron() == null || !CronExpression.isValidExpression(job.getCron())) return result;
        try {
            var cron = new CronExpression(job.getCron()); cron.setTimeZone(TimeZone.getTimeZone(zone));
            var cursor = Date.from(start.isAfter(now) ? start.minusMillis(1) : now);
            var seconds = cronSeconds(job.getCron());
            for (var next = cron.getTimeAfter(cursor); next != null && next.toInstant().isBefore(end); ) {
                if (seconds == null) {
                    result.add(next.toInstant().atZone(zone).toLocalTime().toSecondOfDay());
                    next = cron.getTimeAfter(next);
                    continue;
                }
                // Quartz 决定有效分钟；该分钟内的触发秒由 Cron 的秒字段展开。
                var minute = next.toInstant().atZone(zone).truncatedTo(ChronoUnit.MINUTES).toInstant();
                for (int second : seconds) {
                    var instant = minute.plusSeconds(second);
                    if (!instant.isBefore(next.toInstant()) && instant.isBefore(end))
                        result.add(instant.atZone(zone).toLocalTime().toSecondOfDay());
                }
                next = cron.getTimeAfter(Date.from(minute.plusSeconds(59)));
            }
        } catch (java.text.ParseException ignored) { /* 旧数据中的无效表达式不阻断其他任务。 */ }
        return result;
    }

    /** 未识别的秒字段回退到逐次 Quartz 计算。 */
    private static List<Integer> cronSeconds(String expression) {
        try {
            var values = new TreeSet<Integer>();
            for (var part : expression.trim().split("\\s+")[0].split(",")) {
                var stepParts = part.split("/", -1);
                if (stepParts.length > 2) return null;
                int step = stepParts.length == 2 ? Integer.parseInt(stepParts[1]) : 1;
                if (step <= 0) return null;
                var range = stepParts[0];
                int first, last;
                if (range.equals("*")) { first = 0; last = 59; }
                else if (range.contains("-")) {
                    var bounds = range.split("-", -1);
                    if (bounds.length != 2) return null;
                    first = Integer.parseInt(bounds[0]); last = Integer.parseInt(bounds[1]);
                } else { first = Integer.parseInt(range); last = stepParts.length == 2 ? 59 : first; }
                if (first < 0 || last > 59 || first > last) return null;
                for (int second = first; second <= last; second += step) values.add(second);
            }
            return new ArrayList<>(values);
        } catch (RuntimeException e) { return null; }
    }

    public List<LocalDate> calendar(String month) {
        YearMonth value;
        try {
            if (month == null || !month.matches("\\d{4}-\\d{2}")) throw new IllegalArgumentException();
            value = YearMonth.parse(month);
        } catch (DateTimeException | IllegalArgumentException e) { throw new BuzzException("月份格式应为 yyyy-MM"); }
        var zone = ZoneId.systemDefault(); var now = Instant.now(); var result = new TreeSet<LocalDate>();
        for (var job : jobs.selectList(null)) {
            if (!Boolean.TRUE.equals(job.getStatus())) continue;
            if (job.getCron() == null || !CronExpression.isValidExpression(job.getCron())) continue;
            try {
                var cron = new CronExpression(job.getCron()); cron.setTimeZone(TimeZone.getTimeZone(zone));
                for (int i = 1; i <= value.lengthOfMonth(); i++) {
                    var day = value.atDay(i); var start = day.atStartOfDay(zone).toInstant(); var end = day.plusDays(1).atStartOfDay(zone).toInstant();
                    if (!end.isAfter(now)) continue;
                    var next = cron.getTimeAfter(Date.from(start.isAfter(now) ? start.minusMillis(1) : now));
                    if (next != null && next.toInstant().isBefore(end)) result.add(day);
                }
            } catch (java.text.ParseException ignored) { /* 跳过无效的历史表达式。 */ }
        }
        return List.copyOf(result);
    }
}
