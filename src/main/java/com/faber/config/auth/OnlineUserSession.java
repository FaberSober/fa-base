package com.faber.config.auth;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

/** 仅在服务端 Redis 索引中保存，禁止作为 API 返回值。 */
@Data
public class OnlineUserSession implements Serializable {
    private String id;
    @ToString.Exclude
    private String token;
    private String userId;
    private String username;
    private String name;
    private Long loginTime;
    private long lastAccessTime;
    private String ip;
    private String browser;
    private String os;
    private Long expiresAt;
}
