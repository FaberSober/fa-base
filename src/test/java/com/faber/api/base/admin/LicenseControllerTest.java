package com.faber.api.base.admin;

import com.faber.api.base.admin.rest.LicenseController;
import com.faber.api.base.admin.vo.ret.LicenseRecoveryStatusVo;
import com.faber.api.base.admin.vo.ret.LicenseStatusVo;
import com.faber.core.exception.BuzzException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void matchingMachineEnablesOfflineRecoveryWithoutLogin() {
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.OFFLINE);
        LicenseManager manager = mock(LicenseManager.class);
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        when(manager.getState()).thenReturn(LicenseState.UNCONFIGURED);
        when(manager.isValid()).thenReturn(false);

        LicenseRecoveryStatusVo result = new LicenseController(properties, manager, () -> "machine-1", service)
                .recoveryInfo(" MACHINE-1 ").getData();

        assertNotNull(result);
        assertTrue(result.isMatched());
        assertTrue(result.isUploadAllowed());
        assertEquals(LicenseMode.OFFLINE, result.getMode());
        assertEquals(LicenseState.UNCONFIGURED, result.getStatus());
    }

    @Test
    void mismatchedMachineCannotUseRecovery() {
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.OFFLINE);
        LicenseManager manager = mock(LicenseManager.class);
        OfflineLicenseService service = mock(OfflineLicenseService.class);

        LicenseRecoveryStatusVo result = new LicenseController(properties, manager, () -> "machine-1", service)
                .recoveryInfo("machine-2").getData();

        assertNotNull(result);
        assertFalse(result.isMatched());
        assertFalse(result.isUploadAllowed());
        assertNull(result.getMode());
        assertNull(result.getStatus());
    }

    @Test
    void onlineModeDoesNotExposeFileRecovery() {
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.ONLINE);
        LicenseManager manager = mock(LicenseManager.class);
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        when(manager.getState()).thenReturn(LicenseState.UNCONFIGURED);
        when(manager.isValid()).thenReturn(false);

        LicenseRecoveryStatusVo result = new LicenseController(properties, manager, () -> "machine-1", service)
                .recoveryInfo("machine-1").getData();

        assertNotNull(result);
        assertTrue(result.isMatched());
        assertFalse(result.isUploadAllowed());
        assertEquals(LicenseMode.ONLINE, result.getMode());
    }

    @Test
    void recoveryImportRequiresMatchingMachineAndInvalidLicense() {
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.OFFLINE);
        LicenseManager manager = mock(LicenseManager.class);
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        when(manager.isValid()).thenReturn(false);
        when(manager.getState()).thenReturn(LicenseState.ACTIVE);

        LicenseController controller = new LicenseController(properties, manager, () -> "machine-1", service);
        MockMultipartFile file = new MockMultipartFile("file", "license.lic", "application/json", "{}".getBytes());

        controller.recoveryImport("machine-1", file);

        verify(service).importLicense("license.lic", "{}".getBytes());
    }

    @Test
    void recoveryImportRejectsMismatchedMachine() {
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.OFFLINE);
        LicenseManager manager = mock(LicenseManager.class);
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        LicenseController controller = new LicenseController(properties, manager, () -> "machine-1", service);
        MockMultipartFile file = new MockMultipartFile("file", "license.lic", "application/json", "{}".getBytes());

        assertThrows(BuzzException.class, () -> controller.recoveryImport("machine-2", file));
        verify(service, org.mockito.Mockito.never()).importLicense(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void recoveryImportRejectsActiveLicense() {
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.OFFLINE);
        LicenseManager manager = mock(LicenseManager.class);
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        when(manager.isValid()).thenReturn(true);
        LicenseController controller = new LicenseController(properties, manager, () -> "machine-1", service);
        MockMultipartFile file = new MockMultipartFile("file", "license.lic", "application/json", "{}".getBytes());

        assertThrows(BuzzException.class, () -> controller.recoveryImport("machine-1", file));
        verify(service, org.mockito.Mockito.never()).importLicense(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }

    @Test
    void recoveryImportRejectsOnlineMode() {
        LicenseProperties properties = new LicenseProperties();
        properties.setMode(LicenseMode.ONLINE);
        LicenseManager manager = mock(LicenseManager.class);
        OfflineLicenseService service = mock(OfflineLicenseService.class);
        when(manager.isValid()).thenReturn(false);
        LicenseController controller = new LicenseController(properties, manager, () -> "machine-1", service);
        MockMultipartFile file = new MockMultipartFile("file", "license.lic", "application/json", "{}".getBytes());

        assertThrows(BuzzException.class, () -> controller.recoveryImport("machine-1", file));
        verify(service, org.mockito.Mockito.never()).importLicense(org.mockito.Mockito.any(), org.mockito.Mockito.any());
    }
}
