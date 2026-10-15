package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.entity.ConfigSys;
import com.faber.api.base.admin.mapper.ConfigSysMapper;
import com.faber.core.enums.ConfigSysQiniuAccessTypeEnum;
import com.faber.core.service.StorageService;
import com.faber.core.utils.FaFileUploadUtils;
import com.faber.core.vo.config.FaConfig;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConfigSysBizTest {

    @Test
    void storageRebuildReadsNewConfigAfterUploadTokenCacheIsCleared() {
        ConfigSysMapper mapper = mock(ConfigSysMapper.class);
        StorageService storage = mock(StorageService.class);
        ConfigSysBiz biz = createBiz(mapper, storage);
        ConfigSys updated = new ConfigSys();
        updated.setId(1);
        updated.setData(new FaConfig());
        updated.getData().setQiniuAccessType(ConfigSysQiniuAccessTypeEnum.PRIVATE);
        when(mapper.updateById(updated)).thenReturn(1);
        doReturn(updated).when(biz).getOne();

        try (MockedStatic<FaFileUploadUtils> uploadUtils = mockStatic(FaFileUploadUtils.class)) {
            doAnswer(invocation -> {
                assertSame(updated.getData(), biz.getConfig());
                uploadUtils.verify(FaFileUploadUtils::clearQiniuUploadTokenCache);
                return null;
            }).when(storage).syncStorageDatabaseConfig();

            assertTrue(biz.updateById(updated));
            verify(storage).syncStorageDatabaseConfig();
        }
    }

    @Test
    void failedUpdateKeepsCachedConfigAndStorage() {
        ConfigSysMapper mapper = mock(ConfigSysMapper.class);
        StorageService storage = mock(StorageService.class);
        ConfigSysBiz biz = createBiz(mapper, storage);
        ConfigSys cached = biz.configSysCache;
        ConfigSys update = new ConfigSys();
        update.setId(99);
        when(mapper.updateById(update)).thenReturn(0);

        try (MockedStatic<FaFileUploadUtils> uploadUtils = mockStatic(FaFileUploadUtils.class)) {
            assertFalse(biz.updateById(update));
            assertSame(cached, biz.configSysCache);
            verifyNoInteractions(storage);
            uploadUtils.verifyNoInteractions();
        }
    }

    private ConfigSysBiz createBiz(ConfigSysMapper mapper, StorageService storage) {
        ConfigSysBiz biz = spy(new ConfigSysBiz());
        ReflectionTestUtils.setField(biz, "baseMapper", mapper);
        ReflectionTestUtils.setField(biz, "storageService", storage);
        ConfigSys cached = new ConfigSys();
        cached.setData(new FaConfig());
        biz.configSysCache = cached;
        return biz;
    }
}
