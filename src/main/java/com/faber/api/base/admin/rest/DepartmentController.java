package com.faber.api.base.admin.rest;

import cn.hutool.core.map.MapUtil;
import com.faber.api.base.admin.biz.DepartmentBiz;
import com.faber.api.base.admin.entity.Department;
import com.faber.api.base.admin.vo.query.DepartmentImportReqVo;
import com.faber.api.base.admin.vo.ret.DepartmentImportPreviewVo;
import com.faber.api.base.admin.vo.ret.DepartmentImportResultVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.annotation.LogNoRet;
import com.faber.core.enums.LogCrudEnum;
import com.faber.core.vo.msg.Ret;
import com.faber.core.web.rest.BaseTreeController;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@FaLogBiz("部门")
@RestController
@RequestMapping("/api/base/admin/department")
public class DepartmentController extends BaseTreeController<DepartmentBiz, Department, String> {

    @Override
    @FaLogOpr(value = "下载部门导入模板", crud = LogCrudEnum.R)
    @LogNoRet
    @PostMapping("/exportTplExcel")
    @ResponseBody
    public void exportTplExcel() throws IOException {
        baseBiz.exportDepartmentImportTemplate();
    }

    @Override
    @FaLogOpr(value = "导入部门Excel", crud = LogCrudEnum.C)
    @PostMapping("/importExcel")
    @ResponseBody
    public Ret<Boolean> importExcel(@RequestBody Map<String, Object> params) {
        DepartmentImportReqVo request = new DepartmentImportReqVo();
        request.setFileId(MapUtil.getStr(params, "fileId"));
        baseBiz.commitDepartmentImport(request);
        return ok();
    }

    @FaLogOpr(value = "预览部门导入", crud = LogCrudEnum.R)
    @PostMapping("/import/preview")
    @ResponseBody
    public Ret<DepartmentImportPreviewVo> previewDepartmentImport(@Valid @RequestBody DepartmentImportReqVo request) {
        return ok(baseBiz.previewDepartmentImport(request));
    }

    @FaLogOpr(value = "提交部门导入", crud = LogCrudEnum.C)
    @PostMapping("/import/commit")
    @ResponseBody
    public Ret<DepartmentImportResultVo> commitDepartmentImport(@Valid @RequestBody DepartmentImportReqVo request) {
        return ok(baseBiz.commitDepartmentImport(request));
    }
}
