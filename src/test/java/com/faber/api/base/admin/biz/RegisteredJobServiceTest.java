package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.flow.RegisteredJobService;
import com.faber.config.quartz.BaseJob;
import com.faber.core.annotation.FaJob;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RegisteredJobServiceTest {
    @FaJob("旧任务") static class Legacy extends BaseJob { protected void run() {} }
    @FaJob(value="新任务", group="业务", description="说明", tags={"同步", "同步", " "}, sort=-1)
    @Deprecated static class Rich extends BaseJob { protected void run() {} }
    @FaJob("不应注册") static class Wrong {}
    static class Unannotated extends BaseJob { protected void run() {} }
    @Test void annotationDefaultsRemainCompatible() {
        FaJob job = Legacy.class.getAnnotation(FaJob.class);
        assertEquals("旧任务", job.value()); assertEquals("", job.group()); assertEquals("", job.description());
        assertEquals(0, job.sort()); assertEquals(0, job.tags().length);
    }
    @Test void catalogFiltersAndReturnsRichMetadataWithoutLosingDeprecatedClasses() {
        var result = RegisteredJobService.describe(List.of(Rich.class, Wrong.class, Unannotated.class, Legacy.class, Rich.class));
        assertEquals(2, result.size());
        var rich = result.stream().filter(item -> item.clazzPath().equals(Rich.class.getName())).findFirst().orElseThrow();
        assertEquals("业务", rich.group()); assertEquals("说明", rich.description());
        assertEquals(List.of("同步"), rich.tags()); assertEquals(-1, rich.sort()); assertTrue(rich.deprecated());
        assertFalse(result.get(0).deprecated());
    }
    @Test void sortingIsStableWithinGroup() {
        var first = RegisteredJobService.describe(List.of(Rich.class, Legacy.class));
        assertEquals(first, RegisteredJobService.describe(List.of(Legacy.class, Rich.class)));
    }
}
