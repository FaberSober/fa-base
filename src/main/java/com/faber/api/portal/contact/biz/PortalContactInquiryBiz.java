package com.faber.api.portal.contact.biz;

import cn.hutool.core.util.StrUtil;
import com.faber.api.portal.contact.entity.PortalContactInquiry;
import com.faber.api.portal.contact.mapper.PortalContactInquiryMapper;
import com.faber.core.context.BaseContextHandler;
import com.faber.core.exception.BuzzException;
import com.faber.core.web.biz.BaseBiz;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PortalContactInquiryBiz extends BaseBiz<PortalContactInquiryMapper, PortalContactInquiry> {

    private static final int HOURLY_LIMIT = 5;
    private static final long WINDOW_SECONDS = 3600;
    private final Map<String, Deque<Instant>> submissions = new ConcurrentHashMap<>();

    public void submit(PortalContactInquiry inquiry) {
        checkRateLimit();
        inquiry.setStatus(0);
        inquiry.setSource("PORTAL");
        inquiry.setCrtUser("portal");
        inquiry.setCrtName(StrUtil.blankToDefault(inquiry.getName(), "Portal访客"));
        save(inquiry);
    }

    private void checkRateLimit() {
        String identity = StrUtil.blankToDefault(BaseContextHandler.getIp(), "unknown");
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SECONDS);
        Deque<Instant> records = submissions.computeIfAbsent(identity, ignored -> new ArrayDeque<>());
        synchronized (records) {
            while (!records.isEmpty() && records.peekFirst().isBefore(cutoff)) {
                records.removeFirst();
            }
            if (records.size() >= HOURLY_LIMIT) {
                throw new BuzzException("提交过于频繁，请稍后再试");
            }
            records.addLast(Instant.now());
        }
    }
}
