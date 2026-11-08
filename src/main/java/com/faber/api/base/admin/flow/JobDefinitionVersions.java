package com.faber.api.base.admin.flow;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.faber.api.base.admin.entity.Job;
import com.faber.api.base.admin.entity.JobDefinitionVersion;
import com.faber.api.base.admin.entity.JobLog;
import com.faber.api.base.admin.mapper.JobDefinitionVersionMapper;
import com.faber.api.base.admin.mapper.JobMapper;
import com.faber.core.exception.BuzzException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import java.util.Objects;
import java.util.TreeSet;

/** 调用 ensureCurrent 前须在同一事务内锁定 base_job 行；版本只插入、不更新、不删除。 */
@Service
public class JobDefinitionVersions {
    @Resource private JobMapper jobs;
    @Resource private JobDefinitionVersionMapper versions;
    private static final ObjectMapper JSON = new ObjectMapper();

    public JobDefinitionVersion ensureCurrent(Job job) {
        ObjectNode definition = parse(job.getFlowDefinition());
        definition.put("name", job.getJobName());
        definition.put("detailLogEnabled", Boolean.TRUE.equals(job.getDetailLogEnabled()));
        String normalized = canonical(definition).toString();
        String hash = DigestUtil.sha256Hex(normalized);
        if (job.getCurrentDefinitionVersionId() != null) {
            JobDefinitionVersion current = require(job.getCurrentDefinitionVersionId(), job.getId());
            if (hash.equals(current.getContentHash()) && normalized.equals(current.getFlowDefinition())) return current;
        }
        JobDefinitionVersion latest = versions.selectOne(new QueryWrapper<JobDefinitionVersion>()
                .eq("job_id", job.getId()).orderByDesc("version_no").last("LIMIT 1"));
        JobDefinitionVersion version = new JobDefinitionVersion();
        version.setJobId(job.getId()); version.setVersionNo(latest == null ? 1 : Math.addExact(latest.getVersionNo(), 1));
        version.setJobName(job.getJobName()); version.setDetailLogEnabled(Boolean.TRUE.equals(job.getDetailLogEnabled()));
        version.setFlowDefinition(normalized); version.setContentHash(hash);
        if (versions.insert(version) != 1) throw new IllegalStateException("创建流程定义版本失败");
        if (jobs.update(null, new UpdateWrapper<Job>().eq("id", job.getId())
                .set("current_definition_version_id", version.getId())) != 1) throw new IllegalStateException("关联流程定义版本失败");
        job.setCurrentDefinitionVersionId(version.getId());
        return version;
    }

    public JsonNode definition(JobLog log) {
        JobDefinitionVersion version = require(log.getDefinitionVersionId(), log.getJobId());
        ObjectNode definition = parse(version.getFlowDefinition());
        if (!Objects.equals(version.getContentHash(), DigestUtil.sha256Hex(canonical(definition).toString())))
            throw new BuzzException("流程历史版本校验失败，不能使用当前配置替代");
        return definition;
    }

    private JobDefinitionVersion require(Integer id, Integer jobId) {
        JobDefinitionVersion version = id == null ? null : versions.selectById(id);
        if (version == null || jobId == null || !jobId.equals(version.getJobId()))
            throw new BuzzException("流程历史版本缺失或归属不符，不能使用当前配置替代");
        return version;
    }

    private static ObjectNode parse(String source) {
        try {
            JsonNode value = source == null ? null : JSON.readTree(source);
            if (value instanceof ObjectNode object) return object;
        } catch (java.io.IOException ignored) { }
        throw new BuzzException("流程定义版本JSON无效");
    }

    /** 对象键排序以忽略 JSON 格式差异；数组保留顺序，避免改变节点执行顺序。 */
    private static JsonNode canonical(JsonNode value) {
        if (value.isObject()) {
            ObjectNode sorted = JSON.createObjectNode();
            TreeSet<String> keys = new TreeSet<>(); value.fieldNames().forEachRemaining(keys::add);
            keys.forEach(key -> sorted.set(key, canonical(value.get(key))));
            return sorted;
        }
        if (value.isArray()) {
            var array = JSON.createArrayNode(); value.forEach(item -> array.add(canonical(item))); return array;
        }
        return value;
    }
}
