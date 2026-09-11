package com.faber.api.base.admin.rest;

import com.faber.api.base.admin.vo.ret.LicenseStatusVo;
import com.faber.core.annotation.FaLogBiz;
import com.faber.core.annotation.FaLogOpr;
import com.faber.core.exception.BuzzException;
import com.faber.core.license.LicenseFileCodec;
import com.faber.core.license.LicenseFileException;
import com.faber.core.license.LicenseInfo;
import com.faber.core.license.LicenseManager;
import com.faber.core.license.LicenseProperties;
import com.faber.core.license.MachineIdProvider;
import com.faber.core.license.OfflineLicenseService;
import com.faber.core.vo.msg.Ret;
import com.faber.core.utils.BaseResHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@FaLogBiz("授权")
@RestController
@RequestMapping("/api/base/admin/license")
public class LicenseController extends BaseResHandler {

    private final LicenseProperties properties;
    private final LicenseManager licenseManager;
    private final MachineIdProvider machineIdProvider;
    private final OfflineLicenseService offlineLicenseService;

    public LicenseController(LicenseProperties properties,
                             LicenseManager licenseManager,
                             MachineIdProvider machineIdProvider,
                             OfflineLicenseService offlineLicenseService) {
        this.properties = properties;
        this.licenseManager = licenseManager;
        this.machineIdProvider = machineIdProvider;
        this.offlineLicenseService = offlineLicenseService;
    }

    @FaLogOpr("查询授权信息")
    @GetMapping("/info")
    public Ret<LicenseStatusVo> info() {
        return ok(statusVo());
    }

    @FaLogOpr("导入离线授权")
    @PostMapping("/import")
    public Ret<LicenseStatusVo> importLicense(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BuzzException("License 文件不能为空");
        }
        if (file.getSize() > LicenseFileCodec.MAX_BYTES) {
            throw new BuzzException("License 文件超过 64 KB 限制");
        }
        try {
            offlineLicenseService.importLicense(file.getOriginalFilename(), file.getBytes());
            return ok(statusVo());
        } catch (LicenseFileException e) {
            throw new BuzzException(e.getMessage());
        } catch (IOException e) {
            throw new BuzzException("读取 License 文件失败");
        }
    }

    @FaLogOpr("刷新授权")
    @PostMapping("/refresh")
    public Ret<LicenseStatusVo> refresh() {
        offlineLicenseService.refresh();
        return ok(statusVo());
    }

    private LicenseStatusVo statusVo() {
        LicenseInfo info = licenseManager.getLicenseInfo();
        LicenseStatusVo result = new LicenseStatusVo();
        result.setEnabled(properties.isEnabled());
        result.setMode(properties.getMode());
        result.setStatus(licenseManager.getState());
        try {
            result.setMachineId(machineIdProvider.getMachineId());
        } catch (Exception ignored) {
            // 授权状态接口不能因机器信息读取失败而不可访问。
        }
        if (info != null) {
            result.setLicenseId(info.getLicenseId());
            result.setProduct(info.getProduct());
            result.setCustomer(info.getCustomer());
            result.setIssuedAt(info.getIssuedAt());
            result.setExpireAt(info.getExpireAt());
        }
        return result;
    }
}
