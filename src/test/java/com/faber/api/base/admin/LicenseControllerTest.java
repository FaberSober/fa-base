package com.faber.api.base.admin;

import com.faber.api.base.admin.rest.LicenseController;
import com.faber.api.base.admin.vo.ret.LicenseStatusVo;
import com.faber.core.license.LicenseManager;
import com.faber.core.license.LicenseMode;
import com.faber.core.license.LicenseProperties;
import com.faber.core.license.LicenseState;
import com.faber.core.license.MachineIdProvider;
import com.faber.core.license.OfflineLicenseService;
import com.faber.core.context.BaseContextHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LicenseControllerTest {

    @AfterEach
    void clearContext() {
        BaseContextHandler.remove();
    }

    @Test
    void superAdminCanViewMachineIdWithoutExposingSecretFields() {
        BaseContextHandler.setUserId("1");
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.OFFLINE);
        LicenseManager manager = mock(LicenseManager.class);
        MachineIdProvider machineIdProvider = () -> "machine-1";
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        when(manager.getState()).thenReturn(LicenseState.UNCONFIGURED);

        LicenseStatusVo result = new LicenseController(properties, manager, machineIdProvider, service)
                .info().getData();

        assertNotNull(result);
        assertTrue(result.isCanViewDiagnostics());
        assertEquals("machine-1", result.getMachineId());
        assertEquals(LicenseState.UNCONFIGURED, result.getStatus());
    }

    @Test
    void regularUserCannotViewLicenseDiagnostics() {
        BaseContextHandler.setUserId("2");
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.OFFLINE);
        LicenseManager manager = mock(LicenseManager.class);
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        when(manager.getState()).thenReturn(LicenseState.UNCONFIGURED);

        LicenseStatusVo result = new LicenseController(properties, manager, () -> "machine-1", service)
                .info().getData();

        assertNotNull(result);
        assertFalse(result.isCanViewDiagnostics());
        assertNull(result.getMachineId());
        assertNull(result.getLicenseId());
        assertNull(result.getCustomer());
        assertNull(result.getIssuedAt());
    }

    @Test
    void importsMultipartLicense() {
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.OFFLINE);
        LicenseManager manager = mock(LicenseManager.class);
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        when(manager.getState()).thenReturn(LicenseState.ACTIVE);
        LicenseController controller = new LicenseController(properties, manager, () -> "machine-1", service);
        MockMultipartFile file = new MockMultipartFile("file", "license.lic", "application/json", "{}".getBytes());

        controller.importLicense(file);

        verify(service).importLicense("license.lic", "{}".getBytes());
    }
}
