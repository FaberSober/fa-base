package com.faber.api.base.admin.biz;

import com.faber.core.constant.FaSetting;
import com.faber.core.enums.ConfigSysStorageActiveEnum;
import com.faber.core.exception.BuzzException;
import com.faber.core.service.ConfigSysService;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.platform.LocalPlusFileStorage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** 本机覆盖不得读取共享数据库设置，也不得重写YAML路径。 */
class FileSaveLocalOverrideTest {
    @Test
    void localOverrideKeepsYamlStorageWithoutReadingSharedConfig() {
        FileSaveBiz biz = new FileSaveBiz();
        biz.faSetting = new FaSetting();
        FaSetting.File file = new FaSetting.File();
        file.setLocalOverride(true);
        file.updateSaveType(ConfigSysStorageActiveEnum.MINIO);
        biz.faSetting.setFile(file);
        biz.configSysService = mock(ConfigSysService.class);
        biz.fileStorageService = mock(FileStorageService.class);
        LocalPlusFileStorage storage = mock(LocalPlusFileStorage.class);
        when(biz.fileStorageService.getFileStorage("local-plus-1")).thenReturn(storage);

        biz.syncStorageDatabaseConfig();

        assertEquals("local-plus", file.getSaveType());
        assertEquals("local-plus-1", file.getSavePlatform());
        verifyNoInteractions(biz.configSysService);
        verify(storage, never()).setStoragePath(anyString());
    }

    @Test
    void missingLocalStorageFailsClearlyAndDefaultRemainsOff() {
        FileSaveBiz biz = new FileSaveBiz();
        biz.faSetting = new FaSetting();
        FaSetting.File file = new FaSetting.File();
        assertFalse(file.isLocalOverride());
        file.setLocalOverride(true);
        biz.faSetting.setFile(file);
        biz.configSysService = mock(ConfigSysService.class);
        biz.fileStorageService = mock(FileStorageService.class);

        assertThrows(BuzzException.class, biz::syncStorageDatabaseConfig);
        verifyNoInteractions(biz.configSysService);
    }
}
