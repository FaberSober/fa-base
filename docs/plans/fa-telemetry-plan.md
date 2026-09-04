# PLAN：轻量级 Telemetry 监控与业务统计模块

> 状态：Phase 4 已完成，Phase 5 未开始
> 范围：fa-admin / fa-portal / fa-pixel-editor
> 目标：建设一套自托管、轻量、统一的客户端异常监控与业务数据统计能力，不依赖 Sentry、Umami 等外部平台。

---

# 1. 项目目标

为 fa-admin 建设统一的 Telemetry 基础能力，用于采集和分析：

* 客户端异常
* 用户登录
* 页面访问
* 功能使用
* 业务操作
* 用户活跃
* 应用版本
* 客户端环境

整体定位：

```text
Telemetry
├─ Error Monitoring
│  ├─ 客户端异常
│  ├─ Issue 聚合
│  └─ Breadcrumb
│
└─ Business Analytics
   ├─ 登录统计
   ├─ 页面访问
   ├─ 功能使用
   ├─ 业务事件
   └─ 数据聚合
```

第一阶段重点支持：

```text
fa-admin        → WEB
fa-portal       → WEB
```

同时为：

```text
fa-pixel-editor → DESKTOP
```

预留完整的数据模型和 Collector API。

当前不建设完整 Sentry、Umami、Mixpanel 或 APM 平台，只实现满足 fa 系列项目自身需要的核心能力。

---

# 功能清单与进度

> 本表用于快速查看 Telemetry 模块的整体功能范围、当前版本规划及执行进度。
> 具体技术设计、数据结构和实现细节以本文档对应章节为准。

| 模块            | 功能                     | 功能详情                                   | 当前规划 | 进度    |
| ------------- | ---------------------- | -------------------------------------- | ---- | ----- |
| Telemetry 基础  | Telemetry 模块结构         | 建立统一 Telemetry 后端及前端基础目录               | 当前开发 | ✅已完成  |
| Telemetry 基础  | 应用管理                   | 管理接入 Telemetry 的 Web、Desktop 等应用       | 当前开发 | ✅已完成  |
| Telemetry 基础  | AppKey                 | 为不同应用提供独立上报标识                          | 当前开发 | ✅已完成  |
| Telemetry 基础  | Client Type            | 支持 WEB、DESKTOP、MOBILE、OTHER            | 当前开发 | ✅已完成  |
| Telemetry 基础  | Environment            | 区分 development、test、staging、production | 当前开发 | ✅已完成  |
| Telemetry 基础  | Release                | 记录客户端版本，用于版本维度分析                       | 当前开发 | ✅已完成  |
| Telemetry 基础  | Session                | 为客户端会话生成统一 Session ID                  | 当前开发 | ✅已完成  |
| Telemetry 基础  | User Context           | 统一记录 userId、tenantId 等用户上下文            | 当前开发 | ✅已完成  |
| Telemetry 基础  | Context                | 使用 JSON 保存客户端运行环境信息                    | 当前开发 | ✅已完成  |
| Telemetry 基础  | Collector API          | 建立统一 `/open/telemetry/*` 数据采集入口        | 当前开发 | ✅已完成  |
| Telemetry 基础  | AppKey 校验              | 校验应用是否合法及是否允许上报                        | 当前开发 | ✅已完成  |
| Telemetry 基础  | 上报参数校验                 | 限制 Message、Stack、Context、Properties 大小 | 当前开发 | ✅已完成  |
| Telemetry 基础  | 异步写入                   | Telemetry 数据异步处理，不阻塞业务请求               | 当前开发 | ✅已完成  |
| Telemetry SDK | SDK 初始化                | 提供统一 `telemetry.init()` 初始化入口          | 当前开发 | ✅已完成  |
| Telemetry SDK | 用户识别                   | 提供 `identify()` 和 `clearUser()`        | 当前开发 | ✅已完成  |
| Telemetry SDK | 页面上报                   | 提供 `telemetry.page()`                  | 当前开发 | ✅已完成  |
| Telemetry SDK | 业务事件上报                 | 提供 `telemetry.track()`                 | 当前开发 | ✅已完成  |
| Telemetry SDK | 异常上报                   | 提供 `telemetry.captureException()`      | 当前开发 | ✅已完成  |
| Telemetry SDK | 统一客户端上下文               | SDK 自动携带应用、版本、用户、Session 等信息           | 当前开发 | ✅已完成  |
| 异常监控          | Error Event            | 保存每一次客户端异常实例                           | 当前开发 | ✅已完成  |
| 异常监控          | Issue                  | 将同类异常聚合为 Issue                         | 当前开发 | ✅已完成  |
| 异常监控          | Fingerprint            | 根据异常特征计算 Fingerprint                   | 当前开发 | ✅已完成  |
| 异常监控          | Issue 状态               | 支持 OPEN、RESOLVED、IGNORED               | 当前开发 | ✅已完成  |
| 异常监控          | window.error           | 自动捕获 Web 运行时异常                         | 当前开发 | ✅已完成  |
| 异常监控          | Promise 异常             | 捕获 `unhandledrejection`                | 当前开发 | ✅已完成  |
| 异常监控          | React ErrorBoundary    | 捕获 React 组件树异常                         | 当前开发 | ✅已完成  |
| 异常监控          | 手动异常上报                 | 支持业务代码主动上报异常                           | 当前开发 | ✅已完成  |
| 异常监控          | Breadcrumb             | 保存异常发生前的关键用户行为                         | 当前开发 | ✅已完成  |
| 异常监控          | Web Context            | 记录 Route、Browser、OS、Viewport 等信息       | 当前开发 | ✅已完成  |
| 异常监控          | Issue 列表               | 查询及筛选聚合后的异常问题                          | 当前开发 | ✅已完成  |
| 异常监控          | Issue 详情               | 查看 Stack、Context、Breadcrumb 等详情        | 当前开发 | ✅已完成  |
| 异常监控          | Error Event 明细         | 查看单次异常实例                               | 当前开发 | ✅已完成  |
| 异常监控          | Resource Error         | 捕获资源加载异常                               | 未来规划 | 🕒待处理 |
| 异常监控          | Fetch/Axios Error      | 捕获部分有价值的网络请求异常                         | 未来规划 | 🕒待处理 |
| 异常监控          | Source Map             | 根据 Source Map 还原生产环境 Stack             | 未来规划 | 🕒待处理 |
| 业务统计          | Stat Event             | 保存用户行为及业务事件明细                          | 当前开发 | ✅已完成  |
| 业务统计          | Login Event            | 统计登录成功、失败及退出行为                         | 当前开发 | ✅已完成  |
| 业务统计          | Page View              | 统计页面 PV、UV 和模块访问情况                     | 当前开发 | ✅已完成  |
| 业务统计          | Action Event           | 记录有统计价值的功能操作                           | 当前开发 | ✅已完成  |
| 业务统计          | Business Event         | 记录实际产生业务结果的关键操作                        | 当前开发 | ✅已完成  |
| 业务统计          | Event Code 规范          | 使用 `domain.resource.action` 统一命名       | 当前开发 | ✅已完成  |
| 业务统计          | Properties             | 使用 JSON 保存业务事件扩展属性                     | 当前开发 | ✅已完成  |
| 业务统计          | Java Track API         | 后端支持主动产生统计事件                           | 当前开发 | ✅已完成  |
| 业务统计          | @StatEvent             | 通过注解低侵入记录业务事件                          | 当前开发 | ✅已完成  |
| 业务统计          | 后端事件异步写入               | 通过 ApplicationEvent + Async 保存统计数据     | 当前开发 | ✅已完成  |
| 业务统计          | Stat Daily             | 保存每日聚合后的统计指标                           | 当前开发 | ✅已完成  |
| 业务统计          | 每日聚合任务                 | 每日汇总上一日业务统计数据                          | 当前开发 | ✅已完成  |
| 业务统计          | 今日实时统计                 | 当日数据直接基于 Stat Event 查询                 | 当前开发 | ✅已完成  |
| 业务统计          | DAU                    | 统计每日活跃用户数                              | 当前开发 | ✅已完成  |
| 业务统计          | 登录用户                   | 统计每日登录用户及登录次数                          | 当前开发 | ✅已完成  |
| 业务统计          | PV / UV                | 统计页面访问量和独立访问用户                         | 当前开发 | ✅已完成  |
| 业务统计          | 业务操作统计                 | 统计关键业务事件数量                             | 当前开发 | ✅已完成  |
| 业务统计          | 模块排行                   | 统计各业务模块使用情况                            | 当前开发 | ✅已完成  |
| 业务统计          | 功能排行                   | 根据 Event Code 统计功能使用排行                 | 当前开发 | ✅已完成  |
| 业务统计          | 7 天趋势                  | 展示近期用户、业务及异常趋势                         | 当前开发 | ✅已完成  |
| 业务统计          | 30 天趋势                 | 展示中期用户、业务及异常趋势                         | 当前开发 | ✅已完成  |
| 业务统计          | 事件明细                   | 查询具体业务统计事件                             | 当前开发 | ✅已完成  |
| 综合分析          | 应用维度分析                 | 按不同 Application 查看数据                   | 当前开发 | ❌未完成  |
| 综合分析          | 客户端维度分析                | 按 WEB、DESKTOP 等维度筛选                    | 当前开发 | ❌未完成  |
| 综合分析          | 环境维度分析                 | 按 production、test 等环境筛选                | 当前开发 | ❌未完成  |
| 综合分析          | Release 维度分析           | 对比不同版本的使用量和异常情况                        | 当前开发 | ❌未完成  |
| 综合分析          | User 关联                | 关联用户业务操作和异常情况                          | 当前开发 | ❌未完成  |
| 综合分析          | Session 关联             | 通过 Session 关联业务行为与异常事件                 | 当前开发 | ❌未完成  |
| Dashboard     | 数据概览                   | 展示 DAU、登录、PV、业务操作等核心指标                 | 当前开发 | ✅已完成  |
| Dashboard     | 异常概览                   | 展示异常数量、Issue、受影响用户等指标                  | 当前开发 | ✅已完成  |
| Dashboard     | 使用趋势                   | 展示用户及业务事件趋势                            | 当前开发 | ✅已完成  |
| Dashboard     | 异常趋势                   | 展示客户端异常变化趋势                            | 当前开发 | ✅已完成  |
| Dashboard     | 模块使用排行                 | 展示高频使用模块                               | 当前开发 | ✅已完成  |
| Dashboard     | 功能使用排行                 | 展示高频业务事件                               | 当前开发 | ✅已完成  |
| 数据安全          | 隐私采集规则                 | 禁止采集密码、Token、Cookie 等敏感数据              | 当前开发 | ❌未完成  |
| 数据安全          | Context 控制             | 限制客户端扩展 Context 内容和大小                  | 当前开发 | ✅已完成  |
| 数据安全          | Properties 控制          | 限制业务扩展属性内容和大小                          | 当前开发 | ✅已完成  |
| 数据安全          | Telemetry Sanitizer    | 对敏感信息进行统一清洗                            | 未来规划 | 🕒待处理 |
| 数据生命周期        | Error Event 保留策略       | 异常明细默认保留 90～180 天                      | 当前开发 | ✅已完成  |
| 数据生命周期        | Stat Event 保留策略        | 业务事件明细默认保留 90～180 天                    | 当前开发 | ✅已完成  |
| 数据生命周期        | Issue 长期保留             | 聚合后的异常 Issue 长期保存                      | 当前开发 | ✅已完成  |
| 数据生命周期        | Stat Daily 长期保留        | 每日聚合统计长期保存                             | 当前开发 | ✅已完成  |
| 数据生命周期        | Retention 配置           | 支持配置 Error/Stat Event 保留天数             | 当前开发 | ✅已完成  |
| 数据生命周期        | 定时清理                   | 自动清除过期 Telemetry 明细数据                  | 当前开发 | ✅已完成  |
| Desktop       | DESKTOP Client Type    | 数据模型支持桌面客户端                            | 当前开发 | ✅已完成  |
| Desktop       | Desktop Context        | 预留 OS、Arch、AppVersion 等环境信息            | 当前开发 | ❌未完成  |
| Desktop       | WebView SDK 兼容         | fa-pixel-editor React 层复用 JS SDK       | 当前开发 | ❌未完成  |
| Desktop       | Rust Error 协议          | 预留 Rust/Tauri 异常上报协议                   | 当前开发 | ❌未完成  |
| Desktop       | Rust Business Event 协议 | 预留 Rust 业务事件上报协议                       | 当前开发 | ❌未完成  |
| Desktop       | Rust SDK               | 封装完整 Rust Telemetry SDK                | 未来规划 | 🕒待处理 |
| Desktop       | Rust panic 捕获          | 自动捕获 Rust panic 并上报                    | 未来规划 | 🕒待处理 |
| Desktop       | Desktop Breadcrumb     | 桌面端完整 Breadcrumb 能力                    | 未来规划 | 🕒待处理 |
| 高级分析          | Session Replay         | 回放用户完整操作过程                             | 未来规划 | 🕒待处理 |
| 高级分析          | Heatmap                | 页面点击及热区统计                              | 未来规划 | 🕒待处理 |
| 高级分析          | Funnel                 | 用户业务转化漏斗                               | 未来规划 | 🕒待处理 |
| 高级分析          | 用户画像                   | 建立用户行为画像                               | 未来规划 | 🕒待处理 |
| 高级分析          | 用户分群                   | 根据行为条件划分用户群体                           | 未来规划 | 🕒待处理 |
| 高级分析          | Retention Cohort       | 用户留存及 Cohort 分析                        | 未来规划 | 🕒待处理 |
| 性能监控          | Web Performance        | 收集页面加载及 Web 性能指标                       | 未来规划 | 🕒待处理 |
| 性能监控          | API Metrics            | 统计客户端 API 请求性能                         | 未来规划 | 🕒待处理 |
| 性能监控          | APM                    | 服务端应用性能监控                              | 未来规划 | 🕒待处理 |
| 性能监控          | Distributed Tracing    | 跨服务调用链追踪                               | 未来规划 | 🕒待处理 |
| 基础设施          | Event 批量上报             | 支持多个 Telemetry Event 批量提交              | 未来规划 | 🕒待处理 |
| 基础设施          | AppKey 限流              | 按应用限制 Collector 请求频率                   | 未来规划 | 🕒待处理 |
| 基础设施          | Event Sampling         | 支持高频事件采样                               | 未来规划 | 🕒待处理 |
| 基础设施          | ClickHouse             | 大规模事件分析存储                              | 未来规划 | 🕒待处理 |
| 基础设施          | Elasticsearch          | 异常日志全文检索                               | 未来规划 | 🕒待处理 |
| 基础设施          | Kafka                  | 大规模 Telemetry 消息流转                     | 未来规划 | 🕒待处理 |

## 进度状态说明

| 状态    | 含义             |
| ----- | -------------- |
| ❌未完成  | 已纳入计划，但尚未开始开发  |
| 🟡进行中 | 当前正在开发         |
| 🔍验证中 | 开发完成，正在测试或人工验证 |
| ⏸️已暂停 | 已开始但暂时停止       |
| 🚫已阻塞 | 存在依赖或问题导致无法继续  |
| 🔄待返工 | 已实现但需要重新修改     |
| 👀待确认 | 方案或实现等待确认      |
| 🕒待处理 | 已记录，当前版本暂不执行   |
| ⚪已取消  | 已决定不再实施        |
| ✅已完成  | 已完成开发及必要验证     |

---

# 2. 核心设计原则

## 2.1 统一基础设施

异常监控和业务统计共享：

```text
Application
Client Type
App Key
Environment
Release
Session
User Context
Collector API
Client SDK
Context
```

避免分别维护：

```text
error-sdk
analytics-sdk
```

统一提供：

```text
telemetry
```

---

## 2.2 领域模型独立

统一采集体系，但异常和业务统计分别存储。

```text
Telemetry
       │
       ├─ Error
       │   ├─ Issue
       │   └─ Error Event
       │
       └─ Analytics
           ├─ Stat Event
           └─ Stat Daily
```

禁止将所有数据放入单个万能事件表。

---

## 2.3 Web 优先，Desktop 预留

当前版本：

```text
设计支持 WEB + DESKTOP
实际开发优先 WEB
Desktop 只实现协议和模型预留
```

后续 fa-pixel-editor 可以直接复用现有 Collector API。

---

## 2.4 轻量优先

第一阶段不引入：

```text
Kafka
RabbitMQ
Elasticsearch
ClickHouse
Flink
复杂埋点平台
复杂用户画像
Session Replay
```

优先使用：

```text
Spring Boot
数据库
异步事件
定时任务
```

完成整体能力。

---

# 3. 总体架构

```text
                 fa-admin / fa-portal
                         │
                  Telemetry SDK
                         │
              ┌──────────┴──────────┐
              │                     │
        captureException          track/page
              │                     │
              ▼                     ▼
        /telemetry/error      /telemetry/event
              │                     │
              └──────────┬──────────┘
                         │
                  Telemetry Collector
                         │
              ┌──────────┴──────────┐
              │                     │
        Error Monitoring       Business Analytics
              │                     │
     ┌────────┴────────┐      ┌─────┴─────┐
     │                 │      │           │
   Issue           ErrorEvent Event     Daily
```

后续 Desktop：

```text
fa-pixel-editor
├─ React / WebView
│      ↓
│ Telemetry JS SDK
│
└─ Rust / Tauri
       ↓
Telemetry HTTP API
```

---

# 4. 模块划分

建议在 `fa-base` 内建设：

```text
fa-base
└─ telemetry
   ├─ common
   │  ├─ app
   │  ├─ context
   │  ├─ collector
   │  └─ session
   │
   ├─ error
   │  ├─ issue
   │  ├─ event
   │  └─ fingerprint
   │
   └─ analytics
      ├─ event
      ├─ aggregation
      └─ dashboard
```

当前不建议单独新增 Maven Module。

如果后续 Telemetry 规模明显扩大，再考虑：

```text
fa-telemetry
```

独立模块。

---

# 5. Application 应用管理

新增统一应用表：

```text
base_telemetry_app
```

用于替代仅服务异常监控的：

```text
base_client_error_app
```

## 5.1 核心字段

```text
id

app_key
app_code
app_name

client_type

enabled
remark

create_time
update_time
```

## 5.2 Client Type

统一定义：

```text
WEB
DESKTOP
MOBILE
OTHER
```

示例：

```text
fa-admin
clientType = WEB

fa-portal
clientType = WEB

fa-pixel-editor
clientType = DESKTOP
```

---

# 6. Telemetry 公共上下文

所有 Error Event 和 Business Event 共享：

```text
appId
appKey
clientType

environment
release

sessionId
userId
tenantId

occurTime

context
```

---

# 7. Environment

统一环境：

```text
development
test
staging
production
```

主要用途：

```text
过滤测试环境数据
版本问题排查
环境差异分析
```

---

# 8. Release

客户端初始化时必须支持：

```text
release
```

例如：

```text
fa-admin 3.2.1
fa-pixel-editor 0.9.2
```

Release 同时写入：

```text
Error Event
Stat Event
```

用于分析：

```text
版本发布
↓
用户使用变化
↓
异常率变化
```

---

# 9. Session

客户端 SDK 初始化时创建：

```text
sessionId
```

同一 Session 下的：

```text
Page View
Business Event
Error Event
```

共享相同 `sessionId`。

主要用于建立：

```text
业务行为
        ↓
异常发生
```

之间的关联。

第一阶段不实现完整 Session Replay。

---

# 10. User Context

登录成功后：

```ts
telemetry.identify({
  userId,
  tenantId
})
```

退出后：

```ts
telemetry.clearUser()
```

之后所有事件自动携带：

```text
userId
tenantId
```

不要求业务代码重复传递。

---

# 11. Context 设计

Context 用于描述：

> 当前事件发生的客户端环境。

不同客户端使用不同内容。

## 11.1 Web

```json
{
  "url": "/system/user",
  "route": "/system/user",
  "browser": "Chrome",
  "browserVersion": "152",
  "os": "Windows",
  "viewport": "1920x1080"
}
```

## 11.2 Desktop

```json
{
  "platform": "windows",
  "osVersion": "Windows 11",
  "arch": "x86_64",
  "appVersion": "0.9.2",
  "tauriVersion": "...",
  "runtime": "desktop"
}
```

Context 统一以可扩展 JSON 保存，避免数据库频繁增加环境字段。

---

# 12. Collector API

统一使用：

```text
/open/telemetry
```

## 12.1 异常事件

```text
POST /open/telemetry/error
```

## 12.2 业务事件

```text
POST /open/telemetry/event
```

后续可扩展：

```text
POST /open/telemetry/events
```

用于批量上报。

---

# 13. Collector 基础要求

Collector 必须支持：

* [ ] 校验 `appKey`
* [ ] 校验应用是否启用
* [ ] 校验 Client Type
* [ ] 校验 Event 大小
* [ ] 限制超长 Message / Stack
* [ ] 限制 Context 大小
* [ ] 限制 Properties 大小
* [ ] 服务端补充接收时间
* [ ] 异步写入数据库
* [ ] Collector 异常不能影响业务请求

后续可以增加：

* [ ] IP 限流
* [ ] AppKey 限流
* [ ] Event Sampling
* [ ] 批量 Collector

---

# 14. Client SDK

统一提供：

```text
telemetry
```

核心接口控制在：

```ts
telemetry.init()
telemetry.identify()
telemetry.clearUser()

telemetry.page()
telemetry.track()

telemetry.captureException()
```

初始化示例：

```ts
telemetry.init({
  appKey: "fa-admin",
  clientType: "WEB",
  environment: "production",
  release: APP_VERSION
})
```

---

# 15. Error Monitoring

## 15.1 自动捕获范围

Web 第一阶段支持：

* [ ] `window.error`
* [ ] `unhandledrejection`
* [ ] React ErrorBoundary
* [ ] 手动 `captureException()`

后续可增加：

* [ ] Resource Error
* [ ] Fetch Error
* [ ] Axios Error

但网络请求失败不要默认全部作为异常上报，避免噪声过大。

---

# 16. Error Event

新增：

```text
base_client_error_event
```

核心字段：

```text
id

app_id
issue_id

client_type
environment
release

session_id
user_id
tenant_id

error_type
message
stack

breadcrumbs
context

occur_time
create_time
```

---

# 17. Issue

新增：

```text
base_client_error_issue
```

Issue 表示同一类型异常的聚合结果。

核心字段：

```text
id

app_id
client_type

fingerprint

title
error_type

status

first_seen_time
last_seen_time

event_count
user_count

latest_release

create_time
update_time
```

---

# 18. Fingerprint

推荐：

```text
appId
+
clientType
+
errorType
+
normalizedMessage
+
stackTopFrame
```

计算：

```text
SHA-256
```

作为：

```text
fingerprint
```

避免相同错误重复创建 Issue。

---

# 19. Issue 状态

建议：

```text
OPEN
RESOLVED
IGNORED
```

第一阶段暂不增加复杂工作流。

---

# 20. Breadcrumb

SDK 内部维护：

```text
Breadcrumb Buffer
```

建议数量：

```text
20～50
```

Breadcrumb 可来源：

```text
PAGE
BUSINESS
CLICK
NAVIGATION
HTTP
CUSTOM
```

发生异常时：

```text
captureException()
```

自动携带最近 Breadcrumb。

例如：

```text
21:01 PAGE      /ai/workflow
21:02 CLICK     create
21:03 BUSINESS  ai.workflow.create
21:04 BUSINESS  ai.workflow.run
21:04 ERROR     TypeError
```

---

# 21. Breadcrumb 与 Business Event 的区别

Breadcrumb：

```text
本地缓存
短生命周期
用于异常上下文
```

Business Event：

```text
发送服务器
长期统计
用于产品分析
```

部分 Business Event 可以同时写入 Breadcrumb。

普通 UI 操作：

```text
按钮点击
输入框 focus
```

默认只作为 Breadcrumb，不上传服务器。

---

# 22. Business Analytics

业务统计主要回答：

```text
多少用户使用系统
用户使用哪些模块
哪些功能使用最多
哪些业务操作发生最多
不同版本使用情况
```

---

# 23. 业务事件类型

建议固定：

```text
LOGIN
PAGE_VIEW
ACTION
BUSINESS
```

---

# 24. LOGIN

登录相关事件：

```text
login.success
login.failed
logout
```

主要统计：

```text
登录用户
登录次数
登录失败
活跃用户
```

---

# 25. PAGE_VIEW

页面访问事件：

```text
page.view
```

核心属性：

```text
route
module
pageTitle
```

主要统计：

```text
PV
UV
模块访问排行
```

第一阶段不建设完整 Web Analytics。

---

# 26. ACTION

用于统计有价值的功能使用。

例如：

```text
user.search

file.preview

workflow.open
workflow.debug
```

ACTION 应谨慎定义，避免大量无意义 UI 埋点。

---

# 27. BUSINESS

真正代表业务结果。

建议 Event Code 使用：

```text
domain.resource.action
```

例如：

```text
ai.workflow.create
ai.workflow.run

ai.image.generate

file.upload
file.download

gis.layer.import

bridge.file.upload
bridge.submit

editor.project.create
editor.image.pixelize
editor.sprite.export
```

---

# 28. 业务事件来源规则

## 前端负责

适合：

```text
PAGE_VIEW
部分 ACTION
```

例如：

```text
页面访问
功能入口使用
界面功能切换
```

## 后端负责

重要业务指标必须以后端实际结果为准。

例如：

```text
创建
删除
上传
下载
提交
生成
执行
发布
导出
```

例如：

```text
用户点击 AI Generate
```

不等于：

```text
AI Generate 成功
```

因此：

```text
ai.image.generate
```

应优先由后端业务成功后产生。

---

# 29. Stat Event

新增：

```text
base_stat_event
```

核心字段：

```text
id

app_id
client_type

environment
release

session_id
user_id
tenant_id

event_type
event_code

module

biz_type
biz_id

result
duration

properties

occur_time
create_time
```

---

# 30. Properties

Properties 用于保存：

> 当前业务事件自身的扩展属性。

例如：

```json
{
  "model": "wan2.7-image",
  "resolution": "1024x1024"
}
```

或者：

```json
{
  "fileType": "shp",
  "fileSize": 10485760
}
```

不要用于保存：

```text
客户端环境
```

客户端环境统一放入：

```text
context
```

---

# 31. Java 服务端统计能力

提供：

```java
telemetry.track(...)
```

推荐增加：

```java
@StatEvent
```

例如：

```java
@StatEvent("ai.workflow.run")
public void runWorkflow() {
}
```

通过：

```text
AOP
+
ApplicationEvent
+
@Async
```

实现异步写入。

业务流程：

```text
业务执行
   ↓
业务成功
   ↓
发布 Stat Event
   ↓
立即返回
   ↓
异步写数据库
```

Telemetry 写入失败不得影响核心业务。

---

# 32. Daily Aggregate

新增：

```text
base_stat_daily
```

用于保存每日聚合数据。

核心字段：

```text
id

stat_date

app_id
client_type
environment

event_type
event_code
module

pv
uv

success_count
fail_count

avg_duration

create_time
update_time
```

---

# 33. 聚合任务

每天执行：

```text
Stat Event
      ↓
Daily Aggregate
```

建议第一阶段：

```text
每日凌晨聚合上一日
```

另外 Dashboard 的当天数据可以：

```text
历史 → base_stat_daily
今天 → base_stat_event
```

避免 Dashboard 查询大量长期明细。

---

# 34. Dashboard

第一阶段提供：

```text
业务统计
├─ 数据概览
└─ 事件明细
```

---

# 35. 数据概览

顶部 KPI：

```text
今日活跃用户
今日登录用户
今日页面访问
今日业务操作
今日异常数量
受影响用户
```

---

# 36. 趋势图

支持：

```text
最近 7 天
最近 30 天
```

展示：

```text
DAU
登录用户
业务操作
异常数量
```

---

# 37. 模块排行

例如：

```text
AI 工作流
文件管理
GIS
用户管理
```

展示：

```text
PV
UV
Business Event
```

---

# 38. 功能排行

例如：

```text
AI Workflow Run
File Upload
File Download
Layer Import
```

按照：

```text
eventCode
```

统计。

---

# 39. Issue 页面

菜单：

```text
客户端异常
├─ Issue
└─ 异常事件
```

Issue 列表支持：

```text
应用
客户端类型
环境
版本
状态
异常类型
首次出现
最后出现
事件数量
受影响用户
```

---

# 40. Issue 详情

展示：

```text
异常名称
异常类型
Message
Stack
应用
客户端
环境
版本
首次出现
最后出现
事件数量
用户数量
```

同时展示：

```text
Context
Breadcrumb
最近 Event
```

不同客户端动态展示 Context。

Web：

```text
Browser
Route
Viewport
OS
```

Desktop：

```text
OS
Architecture
App Version
Runtime
```

---

# 41. 事件明细

业务事件明细支持：

```text
应用
事件类型
事件编码
模块
用户
结果
客户端
环境
版本
发生时间
```

第一阶段只作为排查与验证入口，不承担复杂 BI 查询。

---

# 42. 数据隐私

Telemetry 默认禁止采集：

```text
Password
Token
Cookie
Authorization
完整 Request Body
文件内容
聊天完整内容
AI Prompt 原文
用户敏感输入
```

允许：

```text
userId
tenantId
bizId
route
module
eventCode
release
duration
result
客户端环境
```

---

# 43. 数据清洗

异常 Message / Stack / Context 后续应预留：

```text
TelemetrySanitizer
```

用于清理：

```text
Authorization
Token
Cookie
敏感字段
```

第一阶段至少避免主动采集上述数据。

---

# 44. 数据生命周期

建议：

## Error Event

```text
保留 90～180 天
```

## Stat Event

```text
保留 90～180 天
```

## Issue

```text
长期保留
```

## Stat Daily

```text
长期保留
```

整体策略：

```text
原始明细
   ↓
有限生命周期

聚合数据
   ↓
长期保存
```

---

# 45. 数据清理任务

新增定时任务：

```text
Telemetry Cleanup
```

支持配置：

```text
errorEventRetentionDays
statEventRetentionDays
```

定期删除过期明细。

第一阶段暂不做复杂归档。

如果后续数据量增大，可以与现有日志归档机制统一规划。

---

# 46. fa-pixel-editor Desktop 预留

fa-pixel-editor：

```text
Tauri
├─ React
└─ Rust
```

## React / WebView

直接复用 JS SDK：

```ts
telemetry.init({
  appKey: "fa-pixel-editor",
  clientType: "DESKTOP",
  environment: "production",
  release: APP_VERSION
})
```

支持：

```text
React Error
Unhandled Promise
Business Event
Page/Workspace Event
Breadcrumb
```

---

# 47. Rust / Tauri

Rust 后续直接调用：

```text
POST /open/telemetry/error
POST /open/telemetry/event
```

预留：

```text
Rust Error
Tauri Command Error
panic
文件读写异常
图像处理异常
AI 调用异常
插件异常
```

第一阶段：

```text
只保证 Collector API 和数据模型兼容
```

暂不开发完整 Rust SDK。

---

# 48. fa-pixel-editor 业务事件示例

```text
editor.project.create
editor.project.open

editor.image.import
editor.image.pixelize

editor.ai.generate

editor.sprite.create
editor.sprite.export
```

后续可以统计：

```text
DAU
项目创建数
图片导入数
Pixelize 使用数
AI Generate 使用数
Sprite Export 使用数
异常率
```

---

# 49. 当前明确不做

第一阶段不做：

* [ ] Session Replay
* [ ] Heatmap
* [ ] Funnel
* [ ] 用户画像
* [ ] 用户标签
* [ ] 用户分群
* [ ] Retention Cohort
* [ ] 实时流计算
* [ ] APM
* [ ] Distributed Tracing
* [ ] Server Exception Platform
* [ ] Source Map Server
* [ ] ClickHouse
* [ ] Elasticsearch
* [ ] Kafka
* [ ] 完整 Rust SDK
* [ ] 完整 Mobile SDK

避免项目演化为完整 Sentry / Umami / Mixpanel。

---

# 50. 开发阶段

## Phase 1：Telemetry 基础设施

* [x] 建立 `telemetry` 后端目录
* [x] 创建 `base_telemetry_app`
* [x] 定义 Client Type
* [x] 定义 Environment
* [x] 定义 Release
* [x] 定义 Session
* [x] 定义 User Context
* [x] 定义 Context JSON
* [x] 实现 AppKey 校验
* [x] 建立 `/open/telemetry/*`
* [x] 创建前端 Telemetry SDK 基础结构

---

## Phase 2：客户端异常监控

* [x] 创建 `base_client_error_issue`
* [x] 创建 `base_client_error_event`
* [x] 实现 Fingerprint
* [x] 实现 Issue 聚合
* [x] 实现 `window.error`
* [x] 实现 `unhandledrejection`
* [x] 实现 React ErrorBoundary
* [x] 实现 `captureException`
* [x] 实现 Breadcrumb Buffer
* [x] 实现异常 Collector
* [x] 实现 Issue 列表
* [x] 实现 Issue 详情
* [x] 实现异常 Event 查询

---

## Phase 3：业务统计

* [x] 创建 `base_stat_event`
* [x] 创建 `base_stat_daily`
* [x] 实现 `telemetry.page`
* [x] 实现 `telemetry.track`
* [x] 实现 `telemetry.identify`
* [x] 实现 Login Event
* [x] 实现 Page View
* [x] 实现 Action Event
* [x] 实现 Business Event
* [x] 实现 Java `track()`
* [x] 实现 `@StatEvent`
* [x] 实现异步事件写入
* [x] 实现每日聚合
* [x] 实现数据概览
* [x] 实现 7/30 天趋势
* [x] 实现模块排行
* [x] 实现功能排行
* [x] 实现事件明细

---

## Phase 4：数据生命周期

* [x] 增加 Telemetry retention 配置
* [x] Error Event 定时清理
* [x] Stat Event 定时清理
* [x] 保留 Issue
* [x] 保留 Daily Aggregate
* [x] 清理过程增加日志记录

---

## Phase 5：Desktop 接入预留

* [ ] Application 支持 `DESKTOP`
* [ ] Desktop Context
* [ ] Collector 支持 Desktop
* [ ] fa-pixel-editor React SDK 兼容
* [ ] Rust Error 协议定义
* [ ] Rust Business Event 协议定义

当前阶段不要求：

```text
Rust SDK
panic 自动捕获
Desktop 完整埋点
```

---

# 51. 第一阶段数据库表

最终第一阶段核心表控制在：

```text
base_telemetry_app

base_client_error_issue
base_client_error_event

base_stat_event
base_stat_daily
```

共 5 张核心表。

暂不继续拆分：

```text
session
user
breadcrumb
release
context
```

独立表。

避免过度设计。

---

# 52. 前端目录建议

```text
src/
└─ telemetry/
   ├─ index.ts
   ├─ client.ts
   ├─ context.ts
   ├─ session.ts
   ├─ user.ts
   ├─ analytics.ts
   ├─ error.ts
   └─ breadcrumb.ts
```

业务层只依赖：

```ts
import { telemetry } from "@/telemetry"
```

禁止业务模块直接调用 Telemetry HTTP API。

---

# 53. 后端调用规范

业务代码优先：

```java
telemetry.track(...)
```

或：

```java
@StatEvent(...)
```

禁止业务模块：

```text
直接操作 base_stat_event Mapper
```

所有统计事件统一经过：

```text
Telemetry Service
```

---

# 54. Event Code 规范

统一：

```text
domain.resource.action
```

全部：

```text
小写
英文
点分隔
```

例如：

```text
auth.login.success

ai.workflow.create
ai.workflow.run

ai.image.generate

file.upload
file.download

gis.layer.import

editor.image.pixelize
editor.sprite.export
```

禁止随意使用：

```text
AI_WORKFLOW_RUN
runAiWorkflow
workflow-run
```

多种命名体系。

---

# 55. 成功标准

第一阶段完成后，应能够实现：

### 异常监控

```text
Web 异常自动捕获
↓
服务端接收
↓
Fingerprint 聚合
↓
Issue 查询
↓
查看 Stack / Context / Breadcrumb
```

### 业务统计

```text
用户使用系统
↓
产生 Page / Business Event
↓
服务端统一存储
↓
每日聚合
↓
Dashboard 展示 DAU / PV / 功能排行
```

### 两者关联

```text
Application
Release
Session
User
Client
```

保持统一。

可以从异常事件识别：

```text
什么应用
什么版本
哪个用户
哪个 Session
异常前进行了哪些业务操作
```

---

# 56. 最终目标

建立一套适合 fa 系列项目长期复用的轻量 Telemetry 基础设施：

```text
                    fa-telemetry

      ┌──────────────────┴──────────────────┐
      │                                     │
Error Monitoring                    Business Analytics
      │                                     │
异常发现与排查                     产品使用与业务统计
      │                                     │
      └──────────────────┬──────────────────┘
                         │
                 Application / SDK
                 Context / Session
                 User / Release
                 Collector API
```

长期可服务：

```text
fa-admin
fa-portal
fa-pixel-editor
quant-terminal
其他 Web / Desktop 应用
```

在保持轻量、自托管和低运维成本的前提下，逐步形成：

> 统一应用监控 + 客户端异常追踪 + 用户行为统计 + 产品使用分析

当前版本只完成必要基础能力，不追求完整替代 Sentry、Umami、Mixpanel 或专业 APM 平台。
