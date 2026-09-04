# PLAN 补充：桌面终端异常上报预留

## 1. 模块定位调整

模块内部统一按“客户端异常监控”设计，第一阶段主要实现 Web 前端异常采集。

预留客户端类型：

```text
WEB
DESKTOP
MOBILE
OTHER
```

其中：

```text
fa-admin        → WEB
fa-portal       → WEB
fa-pixel-editor → DESKTOP
```

后台菜单仍可暂时命名：

```text
系统监控
└─ 客户端异常
```

或者第一阶段保持“Web异常”，后续再改名。

---

## 2. Collector API 通用化

不要设计成：

```text
POST /open/web-error/event
```

建议直接使用：

```text
POST /open/client-error/event
```

统一接收所有客户端异常。

核心字段：

```text
appKey
clientType
environment
release

errorType
message
stack

userId
occurTime

context
breadcrumbs
```

Web、Tauri 等客户端通过 `context` 扩展自己的环境信息。

---

## 3. 应用表增加客户端类型

`base_web_error_app` 建议直接调整为：

```text
base_client_error_app
```

或者如果当前项目命名希望保持 Web，也至少增加：

```text
client_type
```

字段。

例如：

```text
WEB
DESKTOP
MOBILE
```

---

## 4. Event 通用环境字段

不要把 Event 数据模型完全固定成：

```text
browser
browser_version
page_url
viewport
```

调整为“公共字段 + 扩展 Context”。

### 公共字段

```text
app_id
issue_id
client_type

environment
release

error_type
message
stack

user_id

occur_time
create_time

breadcrumbs
context
```

### Web Context

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

### Desktop Context

```json
{
  "platform": "windows",
  "osVersion": "Windows 11",
  "arch": "x86_64",
  "appVersion": "0.8.2",
  "tauriVersion": "...",
  "runtime": "desktop"
}
```

这样后续扩展客户端不需要改表结构。

---

## 5. fa-pixel-editor 接入预留

`fa-pixel-editor` 使用：

```text
Tauri
├─ React
└─ Rust
```

因此未来需要支持两类异常。

### React / WebView 异常

可以复用 Web SDK：

```text
window.error
unhandledrejection
React ErrorBoundary
captureException
```

但初始化：

```ts
webError.init({
  appKey: "...",
  clientType: "DESKTOP",
  release: APP_VERSION,
  environment: "production"
})
```

---

### Rust / Tauri 异常

预留通用 HTTP 上报接口，后续 Rust 可以直接调用：

```text
POST /open/client-error/event
```

用于上报：

```text
Rust Error
Tauri Command Error
panic
文件读写异常
图像处理异常
AI 模型调用异常
插件异常
```

第一阶段只预留接口协议，不要求立即实现 Rust SDK。

---

## 6. Issue 聚合增加客户端维度

Fingerprint 建议：

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

避免不同客户端的相似错误被错误合并。

---

## 7. 管理后台筛选

Issue 列表增加：

```text
应用
客户端类型
环境
版本
状态
异常类型
```

客户端类型：

```text
Web
Desktop
Mobile
```

详情页根据 `clientType` 动态展示环境信息。

例如 Web：

```text
Browser
Route
Viewport
```

Desktop：

```text
OS
Architecture
App Version
Runtime
```

---

## 8. 开发阶段调整

### Phase 1

基础模型按多客户端设计：

* [ ] Application 支持 `clientType`
* [ ] Collector API 使用通用客户端异常协议
* [ ] Event Context 使用可扩展 JSON
* [ ] Issue 支持客户端维度聚合

### Phase 2

优先实现 Web：

* [ ] fa-admin Web SDK
* [ ] Web Error 捕获
* [ ] React ErrorBoundary
* [ ] Web Breadcrumb

### 后续

Desktop：

* [ ] fa-pixel-editor React/WebView 异常接入
* [ ] Tauri Runtime Context
* [ ] Rust 异常主动上报
* [ ] Rust panic 捕获
* [ ] Desktop Breadcrumb

---

## 9. 当前范围原则

当前版本：

```text
设计支持 Web + Desktop
实际开发优先 Web
Desktop 只预留协议和数据模型
```

避免当前项目复杂度扩大，同时保证后续 `fa-pixel-editor` 可以直接接入，而不需要重新设计服务端。
