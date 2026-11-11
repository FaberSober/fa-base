package com.faber.api.base.admin.vo.ret;

import java.time.LocalDateTime;
import java.util.List;

public record JobTimeline(LocalDateTime serverTime, List<JobTimelineEntry> entries) {}
