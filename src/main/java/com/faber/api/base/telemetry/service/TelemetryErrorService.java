package com.faber.api.base.telemetry.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.faber.api.base.telemetry.entity.ClientErrorEvent;
import com.faber.api.base.telemetry.entity.ClientErrorIssue;
import com.faber.api.base.telemetry.entity.TelemetryApp;
import com.faber.api.base.telemetry.enums.TelemetryIssueStatusEnum;
import com.faber.api.base.telemetry.mapper.ClientErrorEventMapper;
import com.faber.api.base.telemetry.mapper.ClientErrorIssueMapper;
import com.faber.api.base.telemetry.vo.TelemetryErrorReq;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/** 客户端异常落库与 Issue 聚合。 */
@Service
public class TelemetryErrorService {

    private final ClientErrorIssueMapper issueMapper;
    private final ClientErrorEventMapper eventMapper;

    public TelemetryErrorService(ClientErrorIssueMapper issueMapper, ClientErrorEventMapper eventMapper) {
        this.issueMapper = issueMapper;
        this.eventMapper = eventMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public void persist(TelemetryApp app, TelemetryErrorReq request) {
        String fingerprint = fingerprint(app, request);
        ClientErrorIssue issue = findOrCreateIssue(app, request, fingerprint);

        ClientErrorEvent errorEvent = new ClientErrorEvent();
        errorEvent.setAppId(app.getId());
        errorEvent.setIssueId(issue.getId());
        errorEvent.setClientType(request.getClientType());
        errorEvent.setEnvironment(request.getEnvironment());
        errorEvent.setRelease(request.getRelease());
        errorEvent.setSessionId(request.getSessionId());
        errorEvent.setUserId(request.getUserId());
        errorEvent.setTenantId(request.getTenantId());
        errorEvent.setErrorType(request.getErrorType());
        errorEvent.setMessage(request.getMessage());
        errorEvent.setStack(request.getStack());
        errorEvent.setBreadcrumbs(request.getBreadcrumbs());
        errorEvent.setContext(request.getContext());
        errorEvent.setOccurTime(request.getOccurTime());
        errorEvent.setCreateTime(request.getReceiveTime());
        eventMapper.insert(errorEvent);

        refreshIssueAggregate(issue, request);
    }

    private ClientErrorIssue findOrCreateIssue(TelemetryApp app, TelemetryErrorReq request, String fingerprint) {
        ClientErrorIssue issue = findIssue(app.getId(), request.getClientType(), fingerprint);
        if (issue != null) {
            return issue;
        }

        Date occurTime = request.getOccurTime();
        ClientErrorIssue created = new ClientErrorIssue();
        created.setAppId(app.getId());
        created.setClientType(request.getClientType());
        created.setFingerprint(fingerprint);
        created.setTitle(trim(request.getMessage(), 500));
        created.setErrorType(request.getErrorType());
        created.setStatus(TelemetryIssueStatusEnum.OPEN);
        created.setFirstSeenTime(occurTime);
        created.setLastSeenTime(occurTime);
        created.setEventCount(0L);
        created.setUserCount(0L);
        created.setLatestRelease(request.getRelease());
        created.setCreateTime(request.getReceiveTime());
        created.setUpdateTime(request.getReceiveTime());
        try {
            issueMapper.insert(created);
            return created;
        } catch (DuplicateKeyException ignored) {
            return findIssue(app.getId(), request.getClientType(), fingerprint);
        }
    }

    private ClientErrorIssue findIssue(Long appId, com.faber.api.base.telemetry.enums.TelemetryClientTypeEnum clientType, String fingerprint) {
        return issueMapper.selectOne(new LambdaQueryWrapper<ClientErrorIssue>()
                .eq(ClientErrorIssue::getAppId, appId)
                .eq(ClientErrorIssue::getClientType, clientType)
                .eq(ClientErrorIssue::getFingerprint, fingerprint));
    }

    private void refreshIssueAggregate(ClientErrorIssue issue, TelemetryErrorReq request) {
        long eventCount = eventMapper.selectCount(new LambdaQueryWrapper<ClientErrorEvent>()
                .eq(ClientErrorEvent::getIssueId, issue.getId()));
        long userCount = eventMapper.selectObjs(new QueryWrapper<ClientErrorEvent>()
                        .select("user_id")
                        .eq("issue_id", issue.getId())
                        .isNotNull("user_id"))
                .stream()
                .filter(value -> value != null && !value.toString().isBlank())
                .distinct()
                .count();

        issue.setEventCount(eventCount);
        issue.setUserCount(userCount);
        if (issue.getFirstSeenTime() == null || request.getOccurTime().before(issue.getFirstSeenTime())) {
            issue.setFirstSeenTime(request.getOccurTime());
        }
        if (issue.getLastSeenTime() == null || request.getOccurTime().after(issue.getLastSeenTime())) {
            issue.setLastSeenTime(request.getOccurTime());
        }
        issue.setLatestRelease(request.getRelease());
        issue.setUpdateTime(request.getReceiveTime());
        issueMapper.updateById(issue);
    }

    public String fingerprint(TelemetryApp app, TelemetryErrorReq request) {
        String source = app.getId() + "|" + request.getClientType().getValue() + "|"
                + request.getErrorType() + "|" + normalize(request.getMessage()) + "|" + topStackFrame(request.getStack());
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private String normalize(String message) {
        return message == null ? "" : message.trim()
                .replaceAll("0x[0-9a-fA-F]+", "0x#")
                .replaceAll("\\b\\d+\\b", "#")
                .replaceAll("\\s+", " ");
    }

    private String topStackFrame(String stack) {
        if (stack == null || stack.isBlank()) {
            return "";
        }
        for (String line : stack.split("\\R")) {
            String frame = line.trim();
            if (!frame.isEmpty()) {
                return normalize(frame);
            }
        }
        return "";
    }

    private String trim(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
