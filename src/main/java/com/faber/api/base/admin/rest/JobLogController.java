package com.faber.api.base.admin.rest;

import com.faber.api.base.admin.biz.JobLogBiz;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.vo.ret.JobLogReadVo;
import com.faber.api.base.admin.vo.ret.JobFlowLogOrigin;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.vo.msg.Ret;
import com.faber.core.web.rest.BaseController;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * BASE-系统定时任务-执行日志
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2022-09-07 16:03:51
 */
@FaLogBiz("任务日志")
@RestController
@RequestMapping("/api/base/admin/jobLog")
public class JobLogController extends BaseController<JobLogBiz, JobLog, Integer> {
    @GetMapping("/flowOrigin/{id}")
    public Ret<JobFlowLogOrigin> flowOrigin(@PathVariable Integer id, HttpServletResponse response) {
        noLog(response);
        return ok(baseBiz.flowOrigin(id));
    }

    @GetMapping("/read/{id}")
    public Ret<JobLogReadVo> read(@PathVariable Integer id,
                                 @RequestParam(defaultValue = "0") long offset,
                                 @RequestParam(defaultValue = "32768") int maxBytes,
                                 HttpServletResponse response) {
        noLog(response);
        return ok(baseBiz.readLog(id, offset, maxBytes));
    }

    @PostMapping("/download/{id}")
    public void download(@PathVariable Integer id, HttpServletResponse response) throws IOException {
        noLog(response);
        baseBiz.downloadLog(id, response);
    }

    private void noLog(HttpServletResponse response) {
        response.setHeader("FaNoLog", "1");
        response.setHeader("Cache-Control", "private, no-store");
    }
}
