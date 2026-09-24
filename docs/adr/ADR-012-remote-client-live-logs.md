# ADR-012：远程客户端实时日志

- 状态：Proposed
- 日期：2026-09-24
- 范围：`fa-base`、`mobile`、`frontend/apps/admin`
- 目标：管理员可以查看在线客户端，并按需实时查看客户端 Console 日志和 JavaScript 错误；首期接入 UniApp App，协议保留桌面端等客户端扩展能力。

## 1. 功能清单

表格按开发顺序排列，实施时持续更新进度。

| 编号 | 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- | --- |
| 1 | 部署 | WebSocket 部署拓扑确认 | 仓库 Compose 示例仅配置单个服务节点；生产部署拓扑仍需确认 | 示例已确认；生产待确认 | 👀待确认 |
| 2 | `fa-base` WebSocket | 通用客户端注册与在线目录 | 登记客户端类别、运行时、应用、版本和连接状态 | 执行开发 | ✅已完成 |
| 3 | `fa-base` 远程日志 | 采集控制与日志路由 | 管理员按权限启停指定客户端，日志只发给发起会话的管理员 | 执行开发 | 🕒待处理 |
| 4 | `mobile/fa-core-mobile` | 连接与客户端注册 | App 登录且前台时连接；退出、后台或断线时更新在线状态 | 执行开发 | ✅已完成 |
| 5 | `mobile/fa-core-mobile` | Console 与运行时错误采集 | 远程开启后采集各级 Console、未捕获异常和 Promise 异常 | 执行开发 | 🕒待处理 |
| 6 | `frontend/apps/admin` | 在线客户端列表 | 展示在线客户端类别、应用、用户、版本和运行平台 | 执行开发 | ✅已完成 |
| 7 | `frontend/apps/admin` | 实时日志查看 | 选择客户端并启停采集，按级别查看实时日志 | 执行开发 | 🕒待处理 |
| 8 | `fa-base`、`mobile`、`frontend/apps/admin` | 发布包与权限验证 | 验证完整远程日志功能的发布包、权限、脱敏、断线和重连 | 执行开发 | 🕒待处理 |

## 2. 功能开发说明

1. **部署拓扑确认**：仓库 Compose 示例只有一个服务端节点；实际生产拓扑尚未确认。现有 WebSocket Session 保存在进程内。
2. **服务端注册与目录**：复用现有 `/api/websocket/base/{token}` 连接和消息分发。使用通用 `clientType`（首期 `MOBILE`，后续可扩展 `DESKTOP`）及独立 `runtime`（首期 `APP-PLUS`）描述客户端。在线状态由活动连接和心跳维护，不新增数据库表。
3. **采集控制与日志路由**：新增受权限保护的客户端查询、开始和停止接口，并记录管理员操作。开始采集后只向目标客户端发控制消息；客户端日志仅转发给发起采集的管理员，不广播给其他会话。管理员离开、客户端断开或采集超时后停止会话。
4. **移动端连接**：在 `fa-core-mobile` 维护 WebSocket 注册、心跳和重连；仅在登录且 App 前台时保持连接，后台或退出登录时关闭。注册信息使用应用标识、版本、运行平台及现有用户上下文，不采集 IMEI 等持久设备标识。
5. **移动端采集**：仅在管理员开启会话期间转发 `console.debug/log/info/warn/error`、JS 未捕获错误和未处理 Promise 拒绝。保留原有本地 Console 输出；限制单条日志大小和发送速率，清洗密码、Token、Cookie 等敏感内容，不建立无界队列。
6. **管理端页面**：在系统监控区域新增远程客户端日志页面，复用现有管理端 WebSocket 和消息订阅。在线列表通过服务端接口读取，实时日志只保存在当前页面的有界内存缓冲区，不落库。
7. **验证**：验证 App-PLUS 发布包在线登记、远程启停、日志级别、异常上报、断线清理、脱敏和权限拦截；网络或上报失败不得影响客户端业务。

## 3. 决策与边界

- 在线客户端列表与现有“在线用户”区分：后者当前只统计 Web 登录会话；远程诊断以 WebSocket 客户端连接为准。
- 与 Telemetry 已有持久化错误监控分工：本功能提供按需实时日志，不重复建设错误事件存储、Issue 聚合或日志归档。
- 首期只覆盖 App-PLUS JavaScript Console 和 JavaScript 运行时错误；Android 原生 logcat、原生崩溃、后台常驻连接及历史日志查询不在范围内。
- 远程采集默认关闭；查看、开始和停止均需服务端权限校验。客户端断线后自动视为离线并清理采集状态。
- 本轮在线目录按仓库 Compose 单节点示例实现。实际生产若为多实例，需在上线前增加跨节点目录与消息路由，或保证请求始终到达持有连接的实例。
- 现有 WebSocket URL 含 Token 路径，服务端不得记录原始 Token；同时确认代理访问日志对该路径脱敏，否则改用短时连接票据。

## 4. 验收标准

- 管理端能看到当前在线的移动客户端及其类别、应用、版本和运行平台；服务端协议不依赖 `App` 专属类型。
- 管理员可对单个客户端开启/停止采集，只收到该客户端在采集期间产生的日志。
- 客户端本地 Console 正常，未开启采集时不上传打印日志；上报失败不影响业务。
- 断线、退出登录或后台后客户端不再显示在线；重新前台连接后状态恢复。
- 无权限用户不能查看在线列表或控制采集；日志内容有大小限制并脱敏。
- App-PLUS 发布包验证通过；多实例部署前置条件已经确认。

## 5. 相关实现

- 服务端 WebSocket：`fa-base/src/main/java/com/faber/config/websocket/WsChatEndpoint.java`、`WsHolder.java`、`WsBaseService.java`
- 现有服务端日志 tail：`fa-base/src/main/java/com/faber/config/websocket/service/LogTailService.java`（仅作区分，不复用为客户端日志）
- 现有在线用户实现：`fa-base/src/main/java/com/faber/api/base/admin/biz/OnlineUserBiz.java`
- 管理端 WebSocket：`frontend/apps/admin/features/fa-admin-pages/layout/websocket/`
- 移动端 Telemetry 与诊断：`mobile/src/features/fa-core-mobile/telemetry/`、`common/http-logger.ts`、`common/debug-mode.ts`
