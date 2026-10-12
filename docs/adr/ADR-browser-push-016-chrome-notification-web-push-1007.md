# ADR-browser-push-016：Chrome 浏览器通知演示与 Web Push

- 状态：🔍验证中
- 日期：2026-10-07
- 范围：fa-admin-demo-pages、fa-demo、fa-base
- 目标：新增 Chrome 系统通知演示，并建立可在网页关闭后接收测试推送的 Web Push 闭环。

## 1. 功能清单

按页面优先、页面与后台逻辑纵向交付的顺序实施；每项完成后均可人工验证。

| 编号 | 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| ---: | --- | --- | --- | --- | --- |
| WP-01 | fa-admin-demo-pages、fa-demo | 浏览器系统通知演示页 | 新增独立页面和导航入口，查看授权状态并发送本地系统通知 | 本期执行 | ✅已完成 |
| WP-02 | fa-admin-demo-pages、fa-base | 浏览器订阅管理 | 页面提供订阅/取消订阅；后台按登录用户保存当前浏览器订阅 | 本期执行 | ✅已完成 |
| WP-03 | fa-admin-demo-pages、fa-base | Web Push 测试发送 | 页面提交测试消息，后台向该用户的已订阅浏览器发送 | 本期执行 | ✅已完成 |
| WP-04 | fa-admin-demo-pages、fa-base | 通知点击与订阅清理 | 点击通知打开校验后的站内路由；取消订阅并清理失效订阅 | 本期执行 | ✅已完成 |
| WP-05 | fa-base、业务模块 | 业务消息触发 | 将具体业务事件接入 Web Push，遵循用户订阅状态 | 后续规划，触发事件待定 | 🕒待处理 |

## 2. 功能开发说明

1. **演示页与本地通知**：在 frontend/apps/admin/features/fa-admin-demo-pages/pages/admin/demo/advance/browserNotification/ 新增页面，并在 fa-demo 增加菜单升级脚本。页面由用户点击后请求通知权限，并通过 Service Worker 展示标题和正文可配置的系统通知；通知点击跳转由 WP-04 处理。
2. **订阅管理**：注册位于 /admin/ 作用域的 Service Worker；用户明确授权后创建 Push Subscription，并由后台从登录态识别用户、保存或取消订阅。前端仅使用 VAPID 公钥。
3. **测试发送**：页面提供测试发送入口，后台使用 VAPID 私钥通过 Web Push 协议发送；Service Worker 收到 push 事件后展示通知。需能通过另一管理会话向已订阅浏览器发送，以验证接收页面关闭时的行为。
4. **点击与失效处理**：通知点击只打开允许的站内路由；用户取消订阅时同步删除服务端记录，服务端遇到已过期订阅时清理对应记录。
5. **业务接入**：本 ADR 不指定实际业务触发事件。确定事件和目标用户规则后，再接入正式业务消息发送。

## 3. 背景

- 现有 /admin/demo/advance/push 是面向移动端 UniPush 设备的测试台，使用 App 设备标识；浏览器 Web Push 使用独立的 Push Subscription。
- Ant Design 页面内提示只在网页运行时显示，不能替代操作系统级浏览器通知。
- 网页关闭后的推送依赖浏览器 Push API、Service Worker 和服务端发送逻辑，不依赖页面轮询。

## 4. 决策

- 使用标准 Notifications API、Push API 和 Service Worker；Service Worker 统一调用 showNotification() 展示本地测试通知与远程推送。
- 在 frontend/apps/admin/public/admin/browser-notification-sw.js 放置专用 Service Worker，限制其默认作用域为 /admin/，不拦截应用请求。
- 在提供 /api/base/admin 的服务端增加 Web Push 订阅和测试发送接口；订阅与认证用户关联，不能信任前端传入的用户 ID。
- 服务端保存订阅 endpoint、加密公钥和认证密钥；VAPID 私钥只从服务端环境配置读取。新增订阅表时提供 MySQL、PostgreSQL 两种 DDL。
- Web Push 使用独立于 UniPush 的服务端发送实现，不改动现有移动端推送接口。

## 5. 数据流

1. 用户在演示页点击订阅；前端请求通知授权、注册 Service Worker，并创建 Push Subscription。
2. 前端将订阅提交给后台；后台根据登录态保存到该用户名下。
3. 测试发送或后续业务事件触发时，后台向已保存的订阅发送 Web Push。
4. 浏览器唤醒 Service Worker 展示通知；用户点击后聚焦或打开校验过的站内路由。

## 6. 边界与验收

- 首期包含浏览器通知演示、用户订阅管理、测试推送和点击处理；不包含群发运营、定时推送、通知模板和业务事件接入。
- 通知授权必须由用户操作触发。生产环境使用 HTTPS；本地开发可使用 localhost。
- 每个浏览器配置和设备分别订阅。网页标签关闭后可由 Service Worker 处理推送；浏览器或操作系统禁止后台运行或通知时，不承诺即时送达。
- 验收：Chrome 中能查看权限并发送本地通知；可订阅、取消订阅；接收页关闭后从另一会话发送仍能收到测试通知；点击通知进入允许的站内路由；过期订阅会被清理。

## 7. 取舍与替代方案

- 页面内 message/notification 无法满足网页关闭后的接收要求，因此仅作为页面操作反馈，不作为系统通知实现。
- Chrome 扩展专用的 chrome.notifications 不适用于普通网页；现有 UniPush 面向移动 App，也不复用作浏览器订阅。
- 系统通知的具体样式由浏览器和操作系统决定；本功能提供标准字段，不承诺像素级复刻 Chrome UI。

## 8. 参考

- [Notifications API](https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API/Using_the_Notifications_API)
- [Web Push 概览](https://web.dev/articles/push-notifications-overview)
- [订阅 Web Push](https://web.dev/articles/push-notifications-subscribing-a-user)
