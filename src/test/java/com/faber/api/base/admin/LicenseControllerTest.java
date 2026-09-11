package com.faber.api.base.admin;

import com.faber.api.base.admin.rest.LicenseController;
import com.faber.api.base.admin.vo.ret.LicenseStatusVo;
import com.faber.core.license.LicenseManager;
import com.faber.core.license.LicenseMode;
import com.faber.core.license.LicenseProperties;
import com.faber.core.license.LicenseState;
import com.faber.core.license.MachineIdProvider;
import com.faber.core.license.OfflineLicenseService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LicenseControllerTest {

    @Test
    void returnsLicenseStatusWithoutExposingSecretFields() {
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.OFFLINE);
        LicenseManager manager = mock(LicenseManager.class);
        MachineIdProvider machineIdProvider = () -> "machine-1";
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        when(manager.getState()).thenReturn(LicenseState.UNCONFIGURED);

        LicenseStatusVo result = new LicenseController(properties, manager, machineIdProvider, service)
                .info().getData();

        assertNotNull(result);
        assertEquals("machine-1", result.getMachineId());
        assertEquals(LicenseState.UNCONFIGURED, result.getStatus());
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
