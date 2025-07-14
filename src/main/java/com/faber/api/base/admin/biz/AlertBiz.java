package com.faber.api.base.admin.biz;

import com.faber.api.base.admin.cache.constant.RedisCacheConstant;
import com.faber.api.base.admin.entity.Alert;
import com.faber.api.base.admin.mapper.AlertMapper;
import com.faber.api.base.admin.vo.ret.MaterialVo;
import com.faber.core.utils.FaRedisUtils;
import com.faber.core.web.biz.BaseBiz;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BASE-告警信息
 *
 * @author renjinyi
 */
@Service
@Slf4j
public class AlertBiz extends BaseBiz<AlertMapper, Alert> {

    @Autowired
    FaRedisUtils faRedisUtils;

    @Autowired
    private ObjectMapper objectMapper; // 用于对象与JSON的转换

    @Resource
    private AlertMapper alertMapper;

    public Map<String, Integer> selectCountOfType() {
        Map<String, Integer> res = new HashMap<>();
        Map<String, List<Alert>> groupedAlerts = list().stream()
                .collect(java.util.stream.Collectors.groupingBy(Alert::getType));
        groupedAlerts.forEach((type, alerts) -> res.put(type, alerts.size()));

        return res;
    }

    /**
     * 处理原材料库存不足告警内容及Redis缓存
     *
     * @param alert
     */
    private void handleMaterialInventoryAlert(Alert alert) {
        try {
            // 构建Redis的Key（使用常量类前缀）
            String redisKey = buildInventoryAlertRedisKey(alert.getId());

            // 若告警已处理，更新Redis中对应的值
            if (alert.getDeal()) {
                RBucket<String> bucket = faRedisUtils.getRedisson().getBucket(redisKey);
                if (bucket.isExists()) {
                    // 从Redis中获取原有Alert对象并更新状态
                    String alertJson = bucket.get();
                    Alert existingAlert = objectMapper.readValue(alertJson, Alert.class);
                    existingAlert.setDeal(true);
                    existingAlert.setDealTime(alert.getDealTime());
                    existingAlert.setDealStaff(alert.getDealStaff());
                    existingAlert.setDealDesc(alert.getDealDesc());
                    // 重新存入Redis（使用常量类的过期时间）
                    bucket.set(objectMapper.writeValueAsString(existingAlert));
                }
            } else {
                // 未处理的告警直接存入Redis
                faRedisUtils.getRedisson().getBucket(redisKey)
                        .set(objectMapper.writeValueAsString(alert));
            }
        } catch (JsonProcessingException e) {
            log.error("处理库存预警缓存失败", e);
        }
    }

    /**
     * 获取库存不足告警列表（优先从Redis获取，为空则查询数据库并初始化Redis）
     *
     * @return 库存不足告警列表
     */
    public List<Alert> getInventoryShortageAlerts() {
        try {
            //从Redis中查询所有库存预警的Key（通过常量类前缀匹配）
            List<String> redisKeys = new ArrayList<>();
            faRedisUtils.getRedisson().getKeys()
                    .getKeysByPattern(buildInventoryAlertRedisKey("*"))
                    .forEach(redisKeys::add);

            //从Redis中获取所有Alert对象
            List<Alert> alertList = new ArrayList<>();
            if (!redisKeys.isEmpty()) {
                for (String key : redisKeys) {
                    RBucket<String> bucket = faRedisUtils.getRedisson().getBucket(key);
                    String alertJson = bucket.get();
                    if (alertJson != null) {
                        alertList.add(objectMapper.readValue(alertJson, Alert.class));
                    }
                }
                return alertList;
            }

            //查询数据库并初始化Redis缓存
            List<MaterialVo> materialList = alertMapper.checkInventoryShortageAlerts();
            if (materialList != null && !materialList.isEmpty()) {
                for (MaterialVo material : materialList) {
                    Alert alert = new Alert();
                    alert.setType("原材料库存不足");
                    //告警内容
                    String materialName = material.getName();
                    BigDecimal theoryNum = material.getTheoryNum();
                    BigDecimal alertValue = material.getAlertValue();
                    BigDecimal shortageAmount = alertValue.subtract(theoryNum);
                    String content = materialName
                            + "当前库存:" + theoryNum.toPlainString()
                            + ", 不足 " + shortageAmount.toPlainString() + "t";
                    alert.setContent(content);
                    alert.setDeal(false); // 未处理状态

                    // 保存到数据库
//                    this.save(alert);

                    // 暂不设置过期时间
                    String redisKey = buildInventoryAlertRedisKey(alert.getId());
                    faRedisUtils.getRedisson().getBucket(redisKey)
                            .set(objectMapper.writeValueAsString(alert));

                    alertList.add(alert);
                }
            }

            return alertList;
        } catch (JsonProcessingException e) {
            log.error("获取库存预警列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 构建库存预警在Redis中的Key
     *
     * @param alertId 告警ID
     * @return Redis Key
     */
    private String buildInventoryAlertRedisKey(String alertId) {
        return faRedisUtils.buildKey(RedisCacheConstant.INVENTORY_ALERT_KEY_PREFIX + alertId);
    }

    /**
     * 重载：使用Integer类型的alertId构建Key
     */
    public String buildInventoryAlertRedisKey(Integer alertId) {
        return buildInventoryAlertRedisKey(String.valueOf(alertId));
    }
}