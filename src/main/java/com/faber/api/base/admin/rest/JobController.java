package com.faber.api.base.admin.rest;

import com.faber.api.base.admin.vo.ret.JobFlowHistoryStats;
import com.faber.core.vo.msg.TableRet;
import com.faber.api.base.admin.vo.req.JobFlowHistoryQuery;
import cn.hutool.core.map.MapUtil;
import com.faber.api.base.admin.biz.JobBiz;
import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.flow.JobFlowPreflight;
import com.faber.api.base.admin.flow.JobFlowRunner;
import com.faber.api.base.admin.flow.JobFlowRunView;
import com.faber.api.base.admin.entity.JobLog;
import jakarta.annotation.Resource;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.config.annotation.IgnoreUserToken;
import com.faber.core.vo.utils.DictOption;
import com.faber.core.vo.msg.Ret;
import com.faber.core.web.rest.BaseController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

@FaLogBiz("定时任务")
@Controller
@RequestMapping("/api/base/admin/job")
public class JobController extends BaseController<JobBiz, Job, Integer> {

    @Resource private JobFlowRunner flowRunner;

    @GetMapping("/visual/calendar")
    @ResponseBody
    public Ret<List<com.faber.api.base.admin.vo.ret.JobCalendarDay>> visualCalendar(@RequestParam String month) {
        return ok(baseBiz.visualCalendar(month));
    }

    @GetMapping("/visual/timeline")
    @ResponseBody
    public Ret<com.faber.api.base.admin.vo.ret.JobTimeline> visualTimeline(@RequestParam String date, @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status, @RequestParam(required = false) String keyword) {
        return ok(baseBiz.visualTimeline(date, type, status, keyword));
    }

    @GetMapping("/getRegisteredJobs")
    @ResponseBody
    public Ret<List<com.faber.api.base.admin.vo.ret.RegisteredJobVo>> getRegisteredJobs() {
        return ok(baseBiz.getRegisteredJobs());
    }

    @FaLogOpr("手动执行流程")
    @PostMapping("/runFlow/{id}")
    @ResponseBody
    public Ret<JobFlowRunView> runFlow(@PathVariable int id) { return ok(flowRunner.start(id)); }

    @FaLogOpr("核对流程运行状态")
    @PostMapping("/reconcileFlow/{logId}")
    @ResponseBody
    public Ret<JobFlowRunView> reconcileFlow(@PathVariable int logId, @RequestParam int jobId) { return ok(flowRunner.reconcile(logId, jobId)); }

    @FaLogOpr("停止当前流程")
    @PostMapping("/stopFlow/{logId}")
    @ResponseBody
    public Ret<JobFlowRunView> stopFlow(@PathVariable int logId, @RequestParam int jobId) { return ok(flowRunner.stop(logId, jobId)); }

    @GetMapping("/flowRun/{logId}")
    @ResponseBody
    public Ret<JobFlowRunView> flowRun(@PathVariable int logId, @RequestParam(required = false) Integer jobId) { return ok(flowRunner.view(logId, jobId)); }

    @PostMapping("/flowRunsStats/{jobId}")
    @ResponseBody
    public Ret<JobFlowHistoryStats> flowRunsStats(@PathVariable int jobId, @RequestBody JobFlowHistoryQuery request) {
        return ok(flowRunner.historyStats(jobId, request));
    }

    @PostMapping("/flowRunsPage/{jobId}")
    @ResponseBody
    public TableRet<JobLog> flowRunsPage(@PathVariable int jobId, @RequestBody JobFlowHistoryQuery request) {
        return flowRunner.historyPage(jobId, request);
    }

    @GetMapping("/flowRuns/{jobId}")
    @ResponseBody
    public Ret<List<JobLog>> flowRuns(@PathVariable int jobId) { return ok(flowRunner.history(jobId)); }

    @FaLogOpr("流程运行前检查")
    @GetMapping("/preflightFlow/{id}")
    @ResponseBody
    public Ret<JobFlowPreflight> preflightFlow(@PathVariable int id) {
        return ok(baseBiz.preflightFlow(id));
    }

    @FaLogOpr("立即执行")
    @GetMapping("/runOneTime/{id}")
    @ResponseBody
    public Ret<Boolean> runOneTime(@PathVariable long id) {
        baseBiz.runOneTime(id);
        return ok();
    }

    @FaLogOpr("启动")
    @RequestMapping(value = "/startJob/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Ret<Boolean> startJob(@PathVariable int id) {
        baseBiz.startJob(id);
        return ok();
    }

    @FaLogOpr("停止")
    @RequestMapping(value = "/endJob/{id}", method = RequestMethod.GET)
    @ResponseBody
    public Ret<Boolean> endJob(@PathVariable int id) {
        baseBiz.endJob(id);
        return ok();
    }

    @FaLogOpr("获取cron最近5次运行时间")
    @RequestMapping(value = "/quartz/latest", method = RequestMethod.POST)
    @ResponseBody
    @IgnoreUserToken
    public Ret<List<String>> quartzLatest(@RequestBody Map<String, Object> params) throws ParseException  {
        String cron = MapUtil.getStr(params, "cron");
        Integer times = MapUtil.getInt(params, "times");
        List<String> list = baseBiz.quartzLatest(cron, times);
        return ok(list);
    }

    @FaLogOpr("全部任务")
    @GetMapping("/getAllJobs")
    @ResponseBody
    public Ret<List<DictOption<String>>> getAllJobs() {
        List<DictOption<String>> list = baseBiz.getAllJobs();
        return ok(list);
    }

    @FaLogOpr("获取闲置任务")
    @GetMapping("/getIdleJobs")
    @ResponseBody
    public Ret<List<DictOption<String>>> getIdleJobs() {
        List<DictOption<String>> list = baseBiz.getIdleJobs();
        return ok(list);
    }

}
