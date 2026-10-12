package com.faber.api.base.admin.vo.ret;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class RedisKeyDetailVo implements Serializable {

    private String key;

    private String type;

    private Long ttlSeconds;

    private Boolean persistent;

    private Integer size;

    private String valueText;

    private List<Entry> entries;

    private List<StreamEntry> streamEntries;

    private List<ConsumerGroup> consumerGroups;

    @Data
    public static class Entry implements Serializable {

        private Integer index;

        private String field;

        private String value;

        private Double score;
    }

    @Data
    public static class StreamEntry implements Serializable {

        private String id;

        private Long timestamp;

        private Map<String, String> fields;
    }

    @Data
    public static class ConsumerGroup implements Serializable {

        private String name;

        private Integer consumers;

        private Integer pending;

        private String lastDeliveredId;

        private Integer entriesRead;

        private Integer lag;
    }
}
