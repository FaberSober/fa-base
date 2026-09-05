# 在线用户（方案 B）

入口：系统管理 → 系统监控 → 在线用户。仅管理 Sa-Token 的 `web` 会话，保留既有并发登录及共享 Token 策略。

## 使用与权限

- 页面权限：`/admin/system/monitor/onlineUser`。
- 强制下线权限：`/admin/system/monitor/onlineUser:kickout`，同时需要页面权限。
- 仅启用的全局角色（类型 1/2，无租户归属）可授权；租户角色、portal 会话、API Token 均不能调用此模块接口。默认超级管理员可管理。
- 指定会话下线会影响共享同一 Token 的所有浏览器；“全部后台下线”还会处理该用户尚未进入观测索引的 web 会话。portal 和 API Token 不受影响。
- 禁止下线当前会话，也禁止下线当前用户全部后台会话。会话已失效时返回 0，可重复操作。
- 下线在下一次认证请求时生效，前端沿用登录失效跳转；不禁用账号，用户仍可重新登录。
- 下线请求及结果复用操作日志，备注记录目标用户、范围和已完成数量。接口、日志备注均不含原始 Token。

## 部署

1. 发布后端及 `fa-admin-pages` 前端变更。
2. 由现有数据库升级机制执行 `1.0.31_base_online_user.sql`，MySQL/PostgreSQL 分别使用对应目录。没有新增数据库业务表。
3. 已有默认超级管理员角色补齐菜单权限；空库仍由原有 `initAdminRoleMenu` 初始化全部权限。其他平台角色按需分配菜单/操作权限，刷新登录信息后生效。
4. 旧会话在下次成功认证请求时纳入列表，其原始登录时间无法还原时显示“未知”。部署后重新登录的会话实时登记。不能将刚上线时的统计当作全部历史有效 Token 数量。

## 配置与数据口径

若旧登录会话缺少设备索引，接口会提示重新登录并沿用登录失效跳转。升级 Sa-Token Redis 前缀适配器修复后，需重新登录一次以建立完整的会话设备信息；不要通过放行未知来源会话来绕过检查。

`application.yml`：

```yaml
fa:
  online-user:
    active-window-seconds: 300
    touch-interval-seconds: 30
```

有效会话表示未失效的登录，不代表浏览器仍打开。活跃表示最近指定时间内有认证 HTTP 请求，后台轮询也计入。该配置不修改 Sa-Token 的登录有效期或冻结策略。

Redis 使用应用前缀下的 `online-user:sessions`（条目 TTL）与 `online-user:revision`。共享 Token 按一条会话记录，浏览器、操作系统和 IP 均为最近观测值。永不过期 Token 的索引条目也不过期。

查询只读取本模块会话索引，不使用 Redis KEYS 或扫描其他业务键。每节点以 5 秒快照复用会话校验结果，分页/统计不重复读取全索引；登录、退出和下线通过 revision 使快照失效。快照重建仍随已观测会话数线性增长，适用于后台管理会话规模，不面向海量实时连接统计。

采集写入按配置节流，活跃信息可能延迟约一个采样周期加 5 秒。采集异常不会阻断原有登录/请求；下次请求会重试。自然过期依靠 TTL 清理，其他失效会话在快照重建时按 Sa-Token 实际状态清理。

## 定向验收

- 分别使用管理员、仅查看权限的平台用户、租户角色和 portal 登录验证访问范围。
- 登录同账号多个浏览器，确认共享会话数量、最近访问字段和下线影响范围。
- 同账号保留 portal 登录，下线其全部后台会话后确认 portal 仍可访问。
- 验证当前会话保护、重复下线、分页筛选、有效会话/去重用户/活跃用户统计。
- 验证下线后下次认证请求返回登录失效，并在操作日志中确认目标、范围、结果。

自动化测试可运行：

```text
mvn -pl fa-base -am -DskipTests=false -Dtest=OnlineUserBizTest,OnlineUserTrackerTest,OnlineUserSaTokenTest,UserAuthRestInterceptorTest,RequestAgainFilterTest -Dsurefire.failIfNoSpecifiedTests=false test
```

这些测试不需要业务数据库或外部 Redis；Sa-Token 行为测试使用真实框架和内存 DAO。数据库迁移及浏览器联调需在部署环境验收。
