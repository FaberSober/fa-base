package com.faber.api.base.telemetry.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.faber.api.base.admin.biz.ConfigSysBiz;
import com.faber.api.base.telemetry.entity.ClientErrorEvent;
import com.faber.api.base.telemetry.entity.StatEvent;
import com.faber.api.base.telemetry.mapper.ClientErrorEventMapper;
import com.faber.api.base.telemetry.mapper.StatEventMapper;
import com.faber.core.vo.config.FaConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** 清理 Telemetry 原始明细；Issue 与每日聚合数据不参与清理。 */
@Slf4j
@Service
public class TelemetryRetentionService {

    private static final ZoneId ZONE_ID = ZoneId.systemDefault();

    private final ConfigSysBiz configSysBiz;
    private final ClientErrorEventMapper clientErrorEventMapper;
    private final StatEventMapper statEventMapper;

    public TelemetryRetentionService(
            ConfigSysBiz configSysBiz,
            ClientErrorEventMapper clientErrorEventMapper,
            StatEventMapper statEventMapper
    ) {
        this.configSysBiz = configSysBiz;
        this.clientErrorEventMapper = clientErrorEventMapper;
        this.statEventMapper = statEventMapper;
    }

    /** 每日聚合结束后清理到期原始明细。 */
    @Scheduled(cron = "0 30 0 * * ?")
    public void cleanExpiredEvents() {
        FaConfig config = configSysBiz.getConfig();
        cleanErrorEvents(config.getTelemetryErrorEventRetentionDays());
        cleanStatEvents(config.getTelemetryStatEventRetentionDays());
    }

    void cleanErrorEvents(Integer retentionDays) {
        Date cutoffTime = cutoffTime(retentionDays, "异常明细");
        if (cutoffTime == null) {
            return;
        }
        int deleted = clientErrorEventMapper.delete(new LambdaQueryWrapper<ClientErrorEvent>()
                .lt(ClientErrorEvent::getOccurTime, cutoffTime));
        log.info("Telemetry 异常明细清理完成，保留天数：{}，截止时间：{}，删除数量：{}", retentionDays, cutoffTime, deleted);
    }

    void cleanStatEvents(Integer retentionDays) {
        Date cutoffTime = cutoffTime(retentionDays, "统计明细");
        if (cutoffTime == null) {
            return;
        }
        int deleted = statEventMapper.delete(new LambdaQueryWrapper<StatEvent>()
                .lt(StatEvent::getOccurTime, cutoffTime));
        log.info("Telemetry 统计明细清理完成，保留天数：{}，截止时间：{}，删除数量：{}", retentionDays, cutoffTime, deleted);
    }

    private Date cutoffTime(Integer retentionDays, String dataType) {
        if (retentionDays == null || retentionDays < 1) {
            log.warn("Telemetry {}保留天数无效，跳过清理：{}", dataType, retentionDays);
            return null;
        }
        return Date.from(LocalDate.now(ZONE_ID).minusDays(retentionDays).atStartOfDay(ZONE_ID).toInstant());
    }
}
