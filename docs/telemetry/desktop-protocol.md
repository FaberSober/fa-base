# Telemetry Desktop 协议（预留）

本协议供 `fa-pixel-editor` 的 React/WebView 与未来 Rust/Tauri 层共用。当前仅定义 HTTP JSON 协议；不提供 Rust SDK、panic 自动捕获或桌面端完整埋点。

## 接入前提

在 Telemetry 应用管理中创建并启用应用：

- `appKey`：独立上报密钥。
- `clientType`：`DESKTOP`。
- `appCode`：例如 `fa-pixel-editor`。

所有请求发送至 `/api/base/telemetry/open`，无需用户 Token。服务端校验 `appKey`、应用启用状态与 `clientType` 是否一致。

## 公共上下文

异常和业务事件必须共享下列字段：

```json
{
  "appKey": "fa-pixel-editor",
  "clientType": "DESKTOP",
  "environment": "production",
  "release": "0.9.2",
  "sessionId": "desktop-session-id",
  "userId": "42",
  "tenantId": "7",
  "occurTime": "2026-09-05T00:00:00.000Z",
  "context": {
    "platform": "windows",
    "osVersion": "Windows 11",
    "arch": "x86_64",
    "appVersion": "0.9.2",
    "tauriVersion": "2.x",
    "runtime": "desktop"
  }
}
```

`context` 的字段对应 `TelemetryDesktopContext`，允许额外非敏感字段。不得上报密码、Token、Cookie、Authorization、文件内容、完整 AI Prompt 或用户敏感输入。

Web SDK 默认只记录 URL 的 origin 与 pathname，不记录 query string；Desktop 接入也应遵循相同规则。

## Rust/Tauri 异常上报

`POST /api/base/telemetry/open/error`

在公共上下文后追加：

```json
{
  "errorType": "TauriCommandError",
  "message": "导出失败",
  "stack": "optional rust or tauri stack",
  "breadcrumbs": []
}
```

`errorType` 和 `message` 必填；`message` 最长 2,000 字符，`stack` 最长 32,768 字符。`breadcrumbs` 可为空数组。

## Rust/Tauri 业务事件上报

`POST /api/base/telemetry/open/event`

在公共上下文后追加：

```json
{
  "eventType": "BUSINESS",
  "eventCode": "editor.sprite.export",
  "module": "editor",
  "bizType": "sprite",
  "bizId": "optional-business-id",
  "result": "SUCCESS",
  "duration": 320,
  "properties": {
    "format": "png"
  }
}
```

`eventType` 只能为 `LOGIN`、`PAGE_VIEW`、`ACTION`、`BUSINESS`。`eventCode` 必须为小写英文点分格式，例如 `editor.image.pixelize`。`properties` 仅保存业务扩展属性，最大 16 KiB；客户端环境始终放入 `context`。

## WebView 复用

React/WebView 可直接使用现有 JS SDK：

```ts
telemetry.init({
  appKey: 'fa-pixel-editor',
  clientType: 'DESKTOP',
  environment: 'production',
  release: '0.9.2',
  context: {
    platform: 'windows',
    osVersion: 'Windows 11',
    arch: 'x86_64',
    appVersion: '0.9.2',
    runtime: 'desktop',
  },
});
```

同一会话中的 `page`、`track` 和 `captureException` 自动共享 Session、User 与 Desktop Context。
