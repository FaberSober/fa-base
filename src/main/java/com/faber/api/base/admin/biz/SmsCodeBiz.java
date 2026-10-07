package com.faber.api.base.admin.biz;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.faber.api.base.admin.entity.SmsCode;
import com.faber.api.base.admin.mapper.SmsCodeMapper;
import com.faber.api.base.admin.entity.User;
import com.faber.core.utils.FaRedisUtils;
import com.faber.core.web.biz.BaseBiz;
import com.faber.core.exception.BuzzException;
import com.faber.api.base.msg.helper.config.MsgSendConfig;
import com.faber.api.base.msg.helper.config.MsgSendSmsCode;
import com.faber.api.base.msg.helper.properties.SmsConfiguration;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.redisson.api.RLock;
import lombok.extern.slf4j.Slf4j;

import jakarta.annotation.Resource;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 短信验证码
 *
 * @author Farando
 * @email faberxu@gmail.com
 * @date 2019-08-17 20:15:13
 */
@Service
@Slf4j
public class SmsCodeBiz extends BaseBiz<SmsCodeMapper, SmsCode> {

    private static final long LOGIN_CODE_HISTORY_MILLIS = TimeUnit.HOURS.toMillis(1);
    private static final long LOGIN_CODE_COOLDOWN_MILLIS = TimeUnit.SECONDS.toMillis(60);
    private static final int LOGIN_CODE_MAX_PER_HOUR = 5;
    private static final long LOGIN_CODE_VALIDITY_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final int LOGIN_CODE_MAX_FAILED_ATTEMPTS = 5;

    @Resource
    private SmsConfiguration smsConfiguration;

    @Resource
    private UserBiz userBiz;

    @Resource
    private FaRedisUtils faRedisUtils;

    @Value("${fa.portal.auth.sms-login.delivery-mode:disabled}")
    private String loginCodeDeliveryMode;

    @Value("${fa.portal.auth.sms-login.test-phone:}")
    private String loginCodeTestPhone;

    @Value("${fa.portal.auth.sms-login.test-code:}")
    private String loginCodeTestCode;

    public void create(Map<String, Object> params) {
        String phone = MapUtils.getString(params, "phone");

        // 查询是否已经有发送的验证码
        long count = lambdaQuery()
                .eq(SmsCode::getPhone, phone)
                .eq(SmsCode::getPurpose, SmsCode.PURPOSE_GENERAL)
                .count();
        if (count > 0) {
            throw new BuzzException("验证码已发送,请注意查收");
        }

        // 生成验证码
        String code = RandomUtil.randomNumbers(6);

        MsgSendConfig msgSendConfig = MsgSendSmsCode.builder()
                .code(code)
                .build();

        // 发送阿里云短信
        try {
            smsConfiguration.sendSms(phone, msgSendConfig);
        } catch (Exception e) {
            _logger.error(e.getMessage(), e);

            throw new BuzzException("发送短信失败: " + e.getMessage());
        }

        // 验证码入库
        SmsCode smsCode = new SmsCode();
        smsCode.setCode(code);
        smsCode.setPhone(phone);
        smsCode.setPurpose(SmsCode.PURPOSE_GENERAL);
        smsCode.setConsumed(false);
        smsCode.setFailedAttempts(0);
        smsCode.setCrtTime(new Date());
        save(smsCode);
    }

    /**
     * 为已绑定且有效的账户请求登录验证码。对外统一响应，不暴露手机号是否关联账户。
     */
    public void requestLoginCode(String phone) {
        User user = userBiz.getUserByTel(phone);
        if (user == null || !Boolean.TRUE.equals(user.getStatus())) return;

        boolean mockMode = "mock".equalsIgnoreCase(loginCodeDeliveryMode);
        boolean aliyunMode = "aliyun".equalsIgnoreCase(loginCodeDeliveryMode);
        if (!mockMode && !aliyunMode) return;
        if (mockMode && (!phone.equals(loginCodeTestPhone) || !loginCodeTestCode.matches("[0-9]{6}"))) return;

        RLock lock = faRedisUtils.getLock("sms-login:" + DigestUtil.sha256Hex(phone));
        boolean locked = false;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) return;

            Date now = new Date();
            Date historyStart = new Date(now.getTime() - LOGIN_CODE_HISTORY_MILLIS);
            lambdaUpdate()
                    .eq(SmsCode::getPhone, phone)
                    .eq(SmsCode::getPurpose, SmsCode.PURPOSE_LOGIN)
                    .lt(SmsCode::getCrtTime, historyStart)
                    .remove();

            SmsCode latest = baseMapper.selectOne(new LambdaQueryWrapper<SmsCode>()
                    .eq(SmsCode::getPhone, phone)
                    .eq(SmsCode::getPurpose, SmsCode.PURPOSE_LOGIN)
                    .orderByDesc(SmsCode::getCrtTime)
                    .last("LIMIT 1"));
            if (latest != null && latest.getCrtTime() != null
                    && now.getTime() - latest.getCrtTime().getTime() < LOGIN_CODE_COOLDOWN_MILLIS) return;

            long sentCount = lambdaQuery()
                    .eq(SmsCode::getPhone, phone)
                    .eq(SmsCode::getPurpose, SmsCode.PURPOSE_LOGIN)
                    .ge(SmsCode::getCrtTime, historyStart)
                    .count();
            if (sentCount >= LOGIN_CODE_MAX_PER_HOUR) return;

            String code = mockMode ? loginCodeTestCode : RandomUtil.randomNumbers(6);
            if (aliyunMode) {
                MsgSendConfig msgSendConfig = MsgSendSmsCode.builder().code(code).build();
                try {
                    smsConfiguration.sendSms(phone, msgSendConfig);
                } catch (Exception e) {
                    log.warn("Login SMS code delivery failed");
                    return;
                }
            }

            SmsCode smsCode = new SmsCode();
            smsCode.setCode(code);
            smsCode.setPhone(phone);
            smsCode.setPurpose(SmsCode.PURPOSE_LOGIN);
            smsCode.setConsumed(false);
            smsCode.setFailedAttempts(0);
            smsCode.setCrtTime(now);
            save(smsCode);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    /**
     * 验证并单次消费登录验证码。
     */
    public boolean consumeLoginCode(String phone, String code) {
        RLock lock = faRedisUtils.getLock("sms-login:" + DigestUtil.sha256Hex(phone));
        boolean locked = false;
        try {
            locked = lock.tryLock(0, TimeUnit.SECONDS);
            if (!locked) return false;

            Date now = new Date();
            SmsCode latest = baseMapper.selectOne(new LambdaQueryWrapper<SmsCode>()
                    .eq(SmsCode::getPhone, phone)
                    .eq(SmsCode::getPurpose, SmsCode.PURPOSE_LOGIN)
                    .orderByDesc(SmsCode::getCrtTime)
                    .orderByDesc(SmsCode::getId)
                    .last("LIMIT 1"));
            if (latest == null || latest.getCrtTime() == null
                    || Boolean.TRUE.equals(latest.getConsumed())) return false;

            long ageMillis = now.getTime() - latest.getCrtTime().getTime();
            if (ageMillis < 0 || ageMillis >= LOGIN_CODE_VALIDITY_MILLIS) return false;

            int failedAttempts = latest.getFailedAttempts() == null ? 0 : latest.getFailedAttempts();
            if (failedAttempts >= LOGIN_CODE_MAX_FAILED_ATTEMPTS) return false;

            LambdaUpdateWrapper<SmsCode> update = new LambdaUpdateWrapper<SmsCode>()
                    .eq(SmsCode::getId, latest.getId())
                    .eq(SmsCode::getConsumed, false);
            if (latest.getFailedAttempts() == null) {
                update.isNull(SmsCode::getFailedAttempts);
            } else {
                update.eq(SmsCode::getFailedAttempts, failedAttempts);
            }

            if (latest.getCode() != null && latest.getCode().equals(code)) {
                update.set(SmsCode::getConsumed, true);
                return baseMapper.update(null, update) == 1;
            }

            int nextAttempts = failedAttempts + 1;
            update.set(SmsCode::getFailedAttempts, nextAttempts)
                    .set(SmsCode::getConsumed, nextAttempts >= LOGIN_CODE_MAX_FAILED_ATTEMPTS);
            baseMapper.update(null, update);
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (locked && lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    /**
     * 删除失效的验证码
     */
    public void deleteInvalidCode() {
        baseMapper.deleteInvalidCode();
    }

    /**
     * 校验手机号+验证码
     * @param phone
     * @param code
     * @param ifDelete 验证成功后，是否删除验证码
     * @return
     */
    public void validate(String phone, String code, boolean ifDelete) {
        long count = lambdaQuery()
                .eq(SmsCode::getPhone, phone)
                .eq(SmsCode::getCode, code)
                .eq(SmsCode::getPurpose, SmsCode.PURPOSE_GENERAL)
                .count();
        if (count > 0) {
            if (ifDelete) {
                this.deleteCode(phone);
            }

            return;
        }
        throw new BuzzException("短信验证码校验失败");
    }

    public void deleteCode(String phone) {
        lambdaUpdate()
                .eq(SmsCode::getPhone, phone)
                .eq(SmsCode::getPurpose, SmsCode.PURPOSE_GENERAL)
                .remove();
    }

}
