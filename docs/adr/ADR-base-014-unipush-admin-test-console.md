# ADR-014：UniPush 后台测试台

- 状态：✅已完成
- 日期：2026-09-26
- 范围：`fa-base`、`mobile/fa-core-mobile`、`frontend/apps/admin/features/fa-admin-demo-pages`
- 关联：[`ADR-mobile-006-message-push.md`](../../../mobile/docs/adrs/ADR-mobile-006-message-push.md)
- 目标：让开发人员按已注册推送设备发送小范围测试通知，并区分服务端受理、客户端接收和点击事件。

## 1. 功能清单

表格按开发实施顺序排列；进度随实施更新。

| 编号 | 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| ---: | --- | --- | --- | --- | --- |
| 1 | `fa-base` | 推送设备查询 API | 基于 `base_push_device` 查询可用设备，支持用户、App、平台和环境筛选 | 执行开发 | ✅已完成 |
| 2 | `fa-base`、UniCloud | 定向测试发送与状态 API | 经 UniCloud 云函数调用 UniPush 2.0，逐设备返回受理或失败结果并关联测试编号 | 执行开发 | ✅已完成 |
| 3 | `mobile/fa-base-mobile`、`fa-base` | 接收与点击回执 | 将测试通知的接收、点击事件回传服务端；校验后处理应用内跳转 | 执行开发 | ✅已完成 |
| 4 | `fa-admin-demo-pages` | 目标设备选择 | 搜索并勾选已注册设备，默认单设备，单次最多发送 5 台 | 执行开发 | ✅已完成 |
| 5 | `fa-admin-demo-pages` | 测试消息编辑与预览 | 编辑标题、内容、应用内链接、通知栏强制展示开关和可选扩展 JSON，发送前预览确认 | 执行开发 | ✅已完成 |
| 6 | `fa-admin-demo-pages`、`fa-base` | 发送结果与近期记录 | 展示逐设备发送状态和最近 24 小时（最多 50 条）记录；客户端回执接入后展示接收/点击状态 | 执行开发 | ✅已完成 |
| 7 | `fa-base`、`mobile`、管理端 | 端到端验证 | 验证前台接收、后台通知点击、无效设备和无权限场景 | 执行开发 | ✅已完成 |

## 2. 功能开发说明

1. **设备查询**：复用已有 `PushDevice` 实体及 `base_push_device` 表，不复用 `base_user_device`，也不重复建表。查询结果包含用户、平台、App、环境、启用状态和最近上报时间；页面只使用设备记录 ID 定向发送，不返回完整 CID。
2. **测试发送**：提供仅供管理员测试使用的 `POST /api/base/admin/pushDevice/test/send` 和 `/test/status` 接口；校验设备状态、App/环境及最多 5 台目标。Java 服务通过 HMAC 鉴权调用绑定服务空间中的 URL 化云函数，由 `uni-cloud-push` 扩展调用 UniPush 2.0；逐设备返回安全失败原因，`testId` 随推送数据发送，状态在 Redis 保留 24 小时；测试不写入 `base_msg`。
3. **客户端回执**：复用 App 级推送监听，对含 `testId` 的测试消息回传 `received` 或 `clicked` 事件。回执接口从登录态解析用户和设备绑定，不信任客户端传入的用户 ID；点击只允许打开约定的应用内路由。
4. **管理端页面**：新增于 `frontend/apps/admin/features/fa-admin-demo-pages/pages/admin/demo/advance/push/`。设备筛选支持用户、平台、App、环境和启用状态；CID 默认脱敏。测试消息可设置 `force_notification`，每次发送前显示目标数量和消息参数并二次确认。
5. **发送结果**：清楚区分 Provider 已受理、发送失败、客户端已回调和用户已点击；Provider 受理不表示设备已收到。测试运行状态在 Redis 保留 24 小时，不新增长期消息表，也不写入 `base_msg`。
6. **验证**：至少用真实 App 设备验证前台接收和后台点击。推送或回执失败只影响测试结果，不影响登录、公告、站内消息及普通客户端流程。

## 3. 背景

- `fa-base` 已有 `base_push_device` 表、实体、Mapper、Biz 和双数据库 DDL；当前 push 包尚无设备查询 Controller 或 UniPush 发送服务。
- UniApp 诊断页可读取 CID 和推送环境，App 启动监听目前只记录推送回调日志，尚不能把测试消息与客户端回执关联起来。
- `ADR-mobile-006` 规划的是公告和站内消息的正式推送链路；测试台需要单独验证指定设备，避免制造正式站内消息。

## 4. 决策与边界

- 前置依赖：先完成 `ADR-mobile-006` 中登录态设备注册/注销和 UniPush 服务端发送能力；不要求先完成公告与 `MsgHelper` 接入。
- 新增的是开发诊断用途的测试台，作为 `ADR-mobile-006` 中“不新增后台推送页面”决策的有限例外；公告和业务消息仍沿用既有 `MsgHelper` 发送链路。
- 测试台独立使用定向测试接口，不创建 `base_msg`，不调用公告发送，不改变正式消息的保存、发送和点击处理语义。
- 正式消息后续采用 WebSocket 优先、未确认再 UniPush 的设备级规则；测试台始终按所选 CID 直发 UniPush，以便独立验证 Provider、客户端接收和点击，不受在线状态影响。
- 仅使用 UniPush 2.0。Java 服务不再直连个推 REST API，也不配置 UniPush 1.0 的 AppKey/MasterSecret；它只保存 UniCloud 云函数 HTTPS 地址、调用 HMAC 密钥和客户端 AppID。HMAC 密钥同时配置在云函数环境中。云函数部署到 App 绑定的 `fa-admin (alipay)` 服务空间，启用 `uni-cloud-push` 扩展并限制仅允许服务端签名请求。
- `FA_PUSH_UNIPUSH_CLIENT_APP_ID` 使用客户端 `plus.runtime.appid`（`__UNI__...`），用于匹配已注册设备，并由云函数校验后传给 `uniCloud.getPushManager`。接口须有独立管理权限、限制单次目标数量，并校验 App、环境和设备状态；前端不能直接调用云函数或 UniPush Provider。
- 本期只支持即时发送的系统通知、标题、内容、应用内链接和可选扩展 JSON；不做全员/标签推送、定时发送、模板管理、撤回、长期统计和生产运营控制台。

## 5. 数据流

1. App 按 `ADR-mobile-006` 注册或更新推送设备。
2. 管理端查询设备并提交所选设备 ID 与测试消息。
3. 服务端校验权限和设备状态，使用 HMAC 调用 UniCloud 云函数；云函数调用 UniPush 2.0 并返回受理结果，服务端保存短期逐设备测试状态。
4. App 收到或点击含 `testId` 的消息后回传事件；管理端轮询状态并显示结果。

## 6. 验收标准

- 管理员只能从已登记且有效的推送设备中选取目标，单次最多 5 台；越权或无效目标被服务端拒绝。
- 标题、内容和应用内链接可送达选定 App；扩展 JSON 不得覆盖 `testId` 或路由校验字段。
- 结果页分别显示 Provider 受理、客户端回调和点击状态，不把 Provider 受理标记为设备已送达。
- 客户端回执只能关联当前登录账号有效绑定的推送设备；无效路由不会触发任意页面跳转。
- 推送凭据不进入管理端或移动端产物；测试不会产生站内消息或影响公告消息。

## 7. 相关实现

- UniPush 2.0 桥接：`mobile/uniCloud-alipay/cloudfunctions/fa-unipush-send/`
- 推送设备：`fa-base/src/main/java/com/faber/api/base/push/entity/PushDevice.java`、`PushDeviceBiz.java`、`PushDeviceMapper.java`
- 推送设备 DDL：`fa-base/src/main/resources/sql/fa-base/mysql/1.0.41_base_push_device.sql`、`postgre/1.0.41_base_push_device.sql`
- 移动端推送能力与诊断页：`mobile/src/features/fa-core-mobile/push/`、`mobile/src/features/fa-demo-mobile/pages/diagnostics/push/`
- 推送架构与设备注册前置项：`mobile/docs/adrs/ADR-mobile-006-message-push.md`
