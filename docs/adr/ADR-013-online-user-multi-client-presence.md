# ADR-013：多客户端在线用户与设备登记

- 状态：Proposed
- 日期：2026-09-24
- 范围：`fa-base`、`mobile`、`frontend/apps/admin`，后续桌面客户端
- 目标：按用户汇总当前在线的 Web、APP、桌面端设备；支持多设备同时在线，并查看设备明细。

## 1. 功能清单

表格按开发实施顺序排列；实现后持续更新进度。

| 编号 | 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- | --- |
| 1 | 客户端协议 | 统一客户端类型与实例 ID | Web、APP、桌面端上报平台类型和持久化 `clientInstanceId` | 本期实施 | ✅已完成 |
| 2 | `fa-base` 设备管理 | 扩展用户设备登记 | 复用并扩展 `base_user_device`；同一设备重复登录更新记录 | 本期实施 | ✅已完成 |
| 3 | `fa-base` 在线状态 | Redis 在线状态存储 | 按 WebSocket 连接维护心跳，60 秒内有心跳视为在线 | 本期实施 | ✅已完成 |
| 4 | Web、APP（桌面端后续接入） | 登录与 WebSocket 设备上报 | 登录及 APP 恢复持久会话时注册 WebSocket 设备，关联账号、设备及平台信息 | 本期实施 | ✅已完成 |
| 5 | `fa-base` 在线用户 API | 用户汇总与设备明细 | 先按用户聚合再分页；提供按用户查询在线设备的接口 | 本期实施 | ✅已完成 |
| 6 | `frontend/apps/admin` | 在线用户列表改造 | 一行一个用户，显示 Web、APP、桌面端数量；点击设备总数查看明细 | 本期实施 | ✅已完成 |
| 7 | `fa-base`、客户端 | 验证与发布 | 验证心跳过期、断线清理、多设备及跨节点状态 | 本期实施 | 🕒待处理 |
| 8 | `fa-base`、客户端 | 信任设备登录策略 | 用户确认/信任设备，并在签发登录令牌前校验设备 | 留作未来版本规划 | 🕒待处理 |

## 2. 功能开发说明

1. **客户端身份**：新增通用 `FaClientInstanceId` 请求头，并在 WebSocket 注册消息中使用相同 ID。Web 使用浏览器本地持久化随机 ID，APP 使用持久化安装 ID，桌面端使用本机持久化 ID。设备 ID 不使用 IMEI、MAC 等硬件序列号。
2. **设备登记与登录日志**：扩展现有 `base_user_device`，每个用户、客户端类型和设备 ID 维护一条登记记录；首次认证成功后新增，后续登录更新设备信息和最后登录时间。`base_log_login` 已记录登录事件，可补充客户端类型和设备 ID，不另建重复的登录历史表。数据库变更同时提供 MySQL、PostgreSQL 脚本。
3. **Redis 在线状态**：按 WebSocket 连接 ID 保存用户、设备 ID、客户端类型、连接时间和最近心跳，连接打开、注册、心跳时更新，关闭时删除；记录设置 TTL，允许网络中断后自动清理。以 60 秒心跳窗口判断在线；同一设备有多个连接时，只要有一个连接活跃，该设备即在线。
4. **客户端上报**：Web 连接按 `web` 登录来源识别并上报实例 ID；APP 复用现有 `RemoteClientRegister`，补充实例 ID；桌面端沿用相同注册契约。平台类别单独保存，不依赖 Sa-Token 的 `device` 值，因为 APP Portal 登录当前记录为 `portal`。
5. **在线用户 API**：按有效在线设备聚合为用户行，返回 `webCount`、`appCount`、`desktopCount`、`deviceCount` 等汇总字段；分页在聚合之后执行。详情 API 按 `userId` 返回在线设备及应用、版本、平台、系统、型号、连接时间、最近心跳。保留现有权限校验；如保留强制下线，设备与令牌的关联只在服务端处理，不向前端返回令牌。
6. **管理端页面**：主表一行一个用户，三列分别显示 Web、APP、桌面端在线状态和数量；总设备数可点击，打开抽屉懒加载该用户的在线设备列表。沿用现有搜索、分页与操作权限。
7. **验证**：覆盖同一用户多设备、多平台同时在线、重复标签页/连接、60 秒超时、正常关闭、异常断线、旧客户端缺少实例 ID及无权限访问等场景。
8. **信任设备（未来）**：本期只登记设备和在线状态，不阻止未登记设备登录。后续单独设计用户信任确认、撤销及登录校验；不要把设备 ID Header 本身当作可信凭据。

## 3. 决策与边界

- “在线”统一指最近 60 秒内有 WebSocket 心跳；登录令牌仍有效但没有活跃连接的用户不计入在线列表。
- 在线设备数按持久化 `clientInstanceId` 去重，而不是按 WebSocket 连接数计数。旧客户端暂以连接 ID 作为兼容标识，升级后再按实例 ID 去重。
- 在线状态只存 Redis，不将每次心跳写入关系数据库。`base_user_device` 作为持久设备登记，`base_log_login` 作为登录事件记录，Redis 作为短期在线状态，三者职责分离。
- `base_user_device.enable` 继续保留现有访问许可语义；未来的用户信任状态单独设计，避免混用。设备归属按用户和客户端类型约束，不允许普通更新请求静默转移设备所有权。
- 登录 JSON 可继续只包含凭据。服务端从 `HttpServletRequest` 读取客户端类型、实例 ID、版本和 `User-Agent`；稳定实例 ID 必须由客户端提供。`User-Agent`、IP 和自定义 Header 可伪造，只用于识别/展示，不作为信任证明。
- APP 当前登录请求发送 `FaFrom` 和版本 Header，但未发送 `FaAppDeviceId`；现有更新模块的设备 ID 仅用于更新请求。在线登记使用统一实例 ID 契约，避免未协调地触发现有 `UserDeviceInterceptor` 的设备许可逻辑。
- 信任设备校验应在账号密码验证成功后、签发 Token 前执行。当前登录接口绕过普通用户 Token 拦截；现有设备拦截器对缺少设备 ID 的请求会跳过检查，不能直接充当未来的登录信任校验。
- Redis 用于跨实例汇总在线状态。若需要从另一节点强制关闭 WebSocket，还需按连接所属节点路由关闭事件；在线记录 TTL 负责节点异常后的过期清理。

## 4. 验收标准

- 同一用户在 Web、APP、桌面端同时在线时，主表只有一行，三个平台数量和总设备数正确。
- 点击总设备数可查看该用户当前活跃设备明细；任一连接断开或超过心跳窗口后，设备状态按规则更新。
- 重复连接同一设备实例不会重复计设备；旧客户端在兼容期内仍可显示。
- 登录设备信息在凭据验证成功后登记；在线状态不依赖 HTTP 最近请求时间。
- 设备 ID、Token 等敏感凭据不出现在在线用户 API 响应或日志中；信任设备登录拦截不在本 ADR 实施范围内。

## 5. 相关实现

- 现有在线用户：`fa-base/src/main/java/com/faber/api/base/admin/biz/OnlineUserBiz.java`
- 设备登记与拦截：`fa-base/src/main/java/com/faber/api/base/admin/entity/UserDevice.java`、`UserDeviceBiz.java`、`UserDeviceInterceptor.java`
- 登录与登录日志：`fa-base/src/main/java/com/faber/api/base/admin/biz/AuthBiz.java`、`fa-base/src/main/java/com/faber/api/base/admin/entity/LogLogin.java`
- WebSocket 与客户端注册：`fa-base/src/main/java/com/faber/config/websocket/WsChatEndpoint.java`、`fa-base/src/main/java/com/faber/api/base/admin/socket/RemoteClientPresenceService.java`
- APP 请求与注册：`mobile/src/features/fa-core-mobile/common/request.ts`、`common/remote-client.ts`、`update/device.ts`
- 管理端页面：`frontend/apps/admin/features/fa-admin-pages/pages/admin/system/monitor/onlineUser/`
