# 七牛云公开与私有访问支持执行方案

文档简介：为系统设置中的七牛云配置增加公开、私有访问类型，配套文件接口鉴权、私有下载签名和现有调用方兼容，按可独立验证的功能分批开发。

创建时间：2026-10-15。状态：第 1、2、3.1、3.2.1、4、5 项 ✅已完成；第 3.2.2 项图片适配及第 6 项待开发。

| 编号 | 模块 | 功能 | 功能详情 | 当前规划 | 进度 |
| --- | --- | --- | --- | --- | --- |
| 1 | 管理端 / fa-core / fa-base | 访问类型配置闭环 | 页面增加公开、私有选项，配置保存、回显、默认值及配置缓存即时生效 | 本期开发 | ✅已完成 |
| 2 | fa-base / fa-core | 原图、缩略图和文本访问 | 私有文件先认证再访问，复用七牛 SDK 生成短效签名 | 本期开发 | ✅已完成 |
| 3 | 管理端 / @fa/ui / mobile-x | 文件展示与下载 | 按 Web、移动端拆分验收，见 3.1、3.2 | 本期开发 | 🟡进行中 |
| 3.1 | 管理端 / @fa/ui | Web 文件展示与下载 | 同源 Cookie 验证、外部预览短效凭证、退出清理及重新加载 | 本期开发 | ✅已完成 |
| 3.2 | mobile-x | 移动端文件展示与下载 | 按文件流程、图片展示拆分验收，见 3.2.1、3.2.2 | 本期开发 | 🟡进行中 |
| 3.2.1 | fa-base / mobile-x | 移动端上传与凭证预览、下载 | 普通 App 账号通过 Portal 取票，复用 H5 与系统打开，保留上下文隔离 | 本期开发 | ✅已完成 |
| 3.2.2 | mobile-x / fa-base | 移动端图片与头像 | 私有图片、缩略图及头像公共入口适配，凭证过期重取与缓存隔离 | 本期开发 | ❌未完成 |
| 4 | fa-base / @fa/ui | 七牛直传与文件登记 | 上传凭证保持受保护，登记时校验对象并通过 SDK 获取元数据 | 本期开发 | ✅已完成 |
| 5 | fa-base / fa-app | 特殊访问入口兼容 | 登录页素材、OnlyOffice、应用发布下载及 URL 返回值均已验证通过 | 本期开发 | ✅已完成 |
| 6 | 测试 / 文档 | 回归与切换验收 | 定向测试、真实私有桶联调、上线与回退步骤 | 本期开发 | ❌未完成 |
| 7 | 文件权限模型 | 逐文件访问权限 | 文件所有者、角色、租户和业务关联 ACL，独立设计 | 后续版本 | ❌未完成 |

## 1. 规划时确认的现状

- 管理端 `ConfigStorageQiniu.tsx`、后端 `FaConfig` 只有 AK、SK、Bucket、域名、基础路径，没有访问类型；配置保存在 `base_config_sys.data` JSON 中。
- `FileSaveController` 的 `getFile`、`getFilePreview`、`getFileStr` 均带 `@IgnoreUserToken`，直接移除注解会同时改变公开文件及其他存储的行为。
- `FileSaveBiz.downloadFileById`、`getFilePreview` 对远程文件直接重定向到数据库中的 URL，无法处理七牛私有下载。
- 已有 `FilePreviewTicketBiz`：登录后申请一次性 ticket（180 秒），兑换文件会话（600 秒），供 H5 预览及下载使用。当前申请仅检查文件存在及登录状态，没有逐文件授权规则。
- 本地已安装的 `x-file-storage-core 2.2.1` 七牛实现支持 `generatePresignedUrl`，且服务端 `download`、`downloadTh` 已自动签名。优先复用，不新增 SDK 依赖或另写一套下载签名算法。
- `syncUrlQiniu` 通过外链读取文件大小，私有桶下会失败；图片直传组件还存在直接保存、展示外链的用法。
- 调用方包括管理端和 `@fa/ui` 两套文件 service、App 图片及 H5 预览、登录页素材、OnlyOffice 和 `fa-app` 发布下载。

## 2. 推荐行为与范围

- 增加 `qiniuAccessType`，使用 `PUBLIC`、`PRIVATE` 两个值；沿用仓库枚举及表单约定，页面显示“公开”“私有”。历史配置缺失或为 null 时按公开处理，提交非法值应拒绝。
- 该配置表示当前七牛存储的访问方式；不新增每文件访问类型，不改附件表，不迁移已有文件，不新增数据库版本 SQL。
- 私有判断依据文件自身的 `platform` 与七牛配置，而不是仅判断当前激活存储；避免切换到本地后遗留七牛文件失去保护。当前单套七牛配置不扩展为多 Bucket 管理。
- **两层鉴权**：业务端校验调用者资格，存储端生成七牛短效下载签名。禁止匿名用户仅凭任意文件 ID 获得私有下载签名。
- 本期私有文件的普通访问要求有效登录，沿用已有账号状态、应用入口及业务权限校验。`FileSave` 当前不是租户实体，也没有附件 ACL，因此本期不承诺“仅上传者可见”或逐文件租户隔离；需要这些规则时应扩展第 7 项。
- 公开模式及本地、MinIO 的既有访问行为保持兼容。

七牛私有资源需要下载凭证，现有 Java SDK 已提供相关能力，依据：[七牛 Java SDK 下载说明](https://developer.qiniu.com/kodo/sdk/java)。

## 3. 开发说明

### 3.1 页面与配置

- 在七牛配置页增加必选“访问类型”，默认公开，支持保存、回显和重置；私有选项提示需与七牛 Bucket 权限保持一致。
- 同步 `FaConfig`、管理端 `Admin.ConfigSys` 中实际使用的配置类型及必要枚举。
- 调整 `ConfigSysBiz.updateById` 中配置缓存刷新与存储重建顺序；当前先重建存储、后刷新缓存，存在仍使用旧配置的问题。
- AK、SK、Bucket 等上传凭证相关配置变更时清理已有上传 Token 缓存，避免继续使用旧凭证；短效下载地址按请求生成，不保存进配置或文件记录。

### 3.2 文件接口与统一下载地址

| 接口或入口 | 公开模式 | 私有模式 |
| --- | --- | --- |
| `getFile/{fileId}` | 沿用现有行为 | 有效登录或明确授权的资源凭证通过后，302 到短效签名 URL |
| `getFilePreview/{fileId}` | 沿用现有行为 | 同样鉴权，签名缩略图；无有效缩略图时回退原图 |
| `getFileStr/{fileId}` | 沿用现有行为 | 要求有效登录，后端通过已有 SDK 下载后读取 |
| `createPreviewTicket` | 保持登录申请 | 完成同等访问检查后签发，不可成为绕过入口 |
| `exchangePreviewTicket`、`getPreviewFile`、`getPreviewDownload` | 保持 ticket/session 校验 | 保持凭证约束；复用已有 SDK 私有下载能力 |
| `getQiniuUploadToken`、`syncUrlQiniu`、上传接口 | 保持已有鉴权 | 保持已有鉴权，禁止新增匿名上传能力 |

- 将“私有七牛文件需要认证”的条件接入现有鉴权链，复用登录、账号和设备校验及上下文初始化；不在 Biz 内重新实现登录，也不全局删除匿名访问能力。
- 普通 HTTP 文件入口必须先授权，再调用内部下载逻辑；后台任务与已校验的预览会话仍可复用内部存储方法。
- 原图、缩略图及外部服务下载统一复用现有存储签名能力；普通下载签名建议固定 600 秒，不增加管理页面上的时长选项。
- 签名时正确处理中文、空格、路径和图片处理参数；先构建完整资源 URL 再签名，签名后不再整体重新编码或拼接处理参数。
- 仅为已登记、属于配置域名与路径的七牛对象生成签名；不提供接收任意外部 URL 的匿名签名接口。
- 私有文件重定向和含凭证响应禁止被共享缓存；日志不记录完整签名 URL、ticket、session 或密钥。过期和无效凭证应明确拒绝，不回退匿名访问。

### 3.3 展示与直传调用方

- Web 同源图片、链接优先复用现有 Sa-Token Cookie；定向检查 Cookie Path、有效期及资源请求是否能恢复登录上下文。不能带登录态的跨域预览、App 系统下载及第三方访问继续使用短效资源凭证，不把长期登录 Token 拼进 URL。
- 复用现有 ticket/session 流程；确有缩略图需求时最小扩展受限的资源类型，不给任意 URL 签名。凭证到期由已登录调用方重新申请；切换账号时丢弃旧资源缓存。
- 同步处理管理端与 `@fa/ui` 的文件 service、共享图片/上传/预览组件，以及 App 中直接拼接 `getFile`、`getFilePreview` 的公共入口。只修改实际受影响的调用，不逐页面重写。
- `syncUrlQiniu` 验证 URL 的协议、域名及基础路径，解析对象 key，通过已认证的七牛 SDK 查询对象是否存在、大小和 MIME；不再用匿名外链读取大小，不信任前端自报元数据。
- 入库仍保存稳定 URL、对象路径及文件 ID，不保存会过期的签名。图片处理参数应进入签名内容，避免 `UploadImgQiniu` 在私有链接后追加参数导致失效。
- 处理仍保存原始图片外链的既有直传用法；私有模式通过已登记文件 ID 获取授权访问地址，必要时适配其实际调用方，保留公开模式接口兼容。

### 3.4 必须覆盖的特殊入口

- **登录页 Logo、背景图等**：允许未登录访问服务器系统配置中明确指定的展示素材，仅原图/缩略图；不能借此读取任意私有文件或开放文本读取。限制来自服务器配置，不接受客户端声明“这是公开素材”。
- **OnlyOffice**：由已有文档业务入口校验访问后提供短效资源地址，替换其直接拼接匿名 `getFile` 的方式；验证文档服务器首次拉取、重试及编辑后重新打开。
- **应用发布下载**：检查 `AppReleaseBiz` 中两处 `getFileUrl` 调用，保留原有发布、版本及下载资格判断，在已通过业务授权的入口返回短效七牛地址；已有公开发布允许匿名的业务语义继续保留。
- **`getFileUrl` / `getFileFullPath`**：梳理实际调用，仅在需要远程读取的场景提供可访问的私有地址；不能把这些内部方法直接暴露成匿名签名接口，也不能把临时 URL 作为持久业务数据。
- **历史七牛文件**：同一配置下原图、缩略图随访问类型生效；无法还原有效对象路径的历史记录明确报错，不静默改写原数据。

## 4. 主要文件范围

- 页面：`frontend/apps/admin/features/fa-admin-pages/pages/admin/system/base/config/cube/storage/cube/ConfigStorageQiniu.tsx`。
- 配置：`fa-core/.../vo/config/FaConfig.java`、必要枚举、管理端 `types/base/Admin.ts`。
- 后端：`FileSaveController.java`、`FileSaveBiz.java`、`FilePreviewTicketBiz.java`、`ConfigSysBiz.java`、现有鉴权链和上传 Token 工具的必要调整。
- 调用方：管理端及 `@fa/ui` 文件 service、受影响的共享组件、`mobile-x` 文件/图片公共入口、`OnlyofficeBiz.java`、`fa-app/.../AppReleaseBiz.java`。
- 测试及本方案：新增最小定向测试，不执行现有 `FileSaveTest.syncOldFiles` 这类会迁移数据的用例。

## 5. 验证与验收

开发时先执行定向测试及受影响文件的类型/lint 检查，不运行完整前端构建或全量测试。

- 配置：旧 JSON 缺字段及 null 均为公开；非法值拒绝；保存私有后立即生效，重置与重启回显一致。
- 授权：私有原图、缩略图、文本匿名访问失败；正常登录成功；账号冻结、会话过期、无效/过期预览凭证失败；只适用明确登记的公共素材例外。
- 签名：公开地址保持兼容；私有 URL 含短效凭证，过期后七牛拒绝；中文、空格、处理参数及无缩略图回退正常；不签任意域名或路径。
- 上传：后端上传与七牛直传均可完成入库及回显；不存在对象、越界 URL 被拒绝；保存记录不含临时下载凭证。
- 回归：原图、缩略图、文本、图片/视频预览与拖动、文件下载、App/H5 凭证预览、OnlyOffice、登录背景/Logo、应用下载，以及切换激活存储后的历史七牛文件。

需要人工验证的步骤：

1. 使用独立测试 Bucket，分别验证公开和私有，不直接切换生产 Bucket。
2. 在配置页保存私有模式，使用登录账号上传图片、文本、视频及文档，验证展示、缩略图和下载。
3. 无痕窗口打开普通文件接口，确认无法访问；登录页 Logo、背景等指定素材仍可展示。
4. 检查 App/H5、OnlyOffice 和应用发布下载，确认可访问且不依赖裸私有 URL。
5. 等待签名过期，确认旧链接失效，已登录页面可重新获取资源；检查签名未入库、未进入完整请求日志。
6. 回到公开模式并使用公开测试桶验证旧行为；检查本地、MinIO 不受影响。

开发完成后功能项标为 `🔍验证中`；人工验证成功后再更新为 `✅已完成`。

## 6. 上线、回退与限制

- 系统访问类型与七牛控制台的 Bucket 权限必须一致。本期配置不自动修改云端 Bucket ACL；仅在页面选私有不能阻止已有公开外链访问。上线时安排维护窗口协调两端切换，并检查所用 CDN 域名的缓存与访问控制。
- 先发布具备两种模式能力的代码，确认公开回归通过，再进行 Bucket 与系统配置切换；私有切换后验证原有裸链接不可访问，必要时刷新此前的公开 CDN 缓存。
- 回退需同时协调系统配置、Bucket 权限和 CDN；将 Bucket 改回公开会重新开放其文件，不作为自动失败回退策略。
- 私有签名 URL 和预览 session 在有效期内可被持有者使用；账号退出不会立即撤销已经签发的七牛 URL。本期通过短有效期限制窗口，不实现即时逐链接撤销。
- 私有模式控制整套七牛存储访问，不提供单文件永久分享、多 Bucket 配置、文件加密或新增附件 ACL。

## 7. 首批开发与验证记录（2026-10-15，已验证通过）

首批范围：第 1 项“访问类型配置闭环”。该批仅实现配置记录，文件接口与签名的后续开发进展见第 8 节。

- 已增加后端访问类型枚举及配置字段，兼容缺失、null 为公开，拒绝未知值、数字序号等非法输入。
- 已增加七牛配置页选项、保存回显和重置；保存成功后以最新保存值作为重置基准。
- 已调整配置更新顺序：写入成功后先刷新配置缓存、清理上传凭证，再重建存储；写入失败不刷新缓存或重建存储。
- 未修改线上配置、七牛 Bucket 权限及文件记录；用户已确认本批人工验证通过，相关代码及方案纳入本次 Git 提交。

自动验证已通过：

- `mvn -o -q -pl fa-base -am -DskipTests=false -Dtest=FaConfigQiniuTest,ConfigSysBizTest -Dsurefire.failIfNoSpecifiedTests=false test`：5 个测试通过，无失败或跳过；覆盖默认值、JSON 往返、非法输入、更新顺序及更新失败路径。
- 前端对 `ConfigStorageQiniu.tsx`、`types/base/Admin.ts` 定向执行 `biome lint` 通过。
- 相关仓库 `git diff --check` 通过；未执行全量测试、前端构建或浏览器验证。

本批人工验证步骤（使用测试环境，不修改生产 Bucket）：

1. 打开“系统设置 → 文件配置 → 七牛云”，确认已有未配置访问类型的数据默认显示“公开”。
2. 选择“私有”并保存，刷新页面，确认仍显示“私有”；其余七牛配置值应保持不变。
3. 不保存地改为“公开”，点击“重置”，确认恢复为刚保存的“私有”；再保存为“公开”并刷新，确认回显正确。
4. 在测试环境更新七牛凭证或域名后，确认后续获取上传凭证或后端上传使用新配置，无需重启；不要使用生产密钥进行轮换测试。

用户已确认本批验证通过，第 1 项已更新为 `✅已完成`；其他功能保留原进度。

## 8. 第二批开发与验证记录（2026-10-15，已验证通过）

第二批范围：第 2 项完整后端读取流程，及第 5 项中的登录页公共素材例外。该批未涉及第 3、4 项及第 5 项剩余内容；后续开发进展见第 9 节，整体切换仍需完成剩余适配。

### 已开发内容

- 原图、缩略图、文本入口通过 `@FileAccess` 按文件自身存储平台及配置决定是否要求登录；私有七牛文件复用现有登录、账号状态、后台访问资格、上下文初始化及设备校验，公开模式及其他存储保留原有认证行为。
- 私有原图和缩略图生成 600 秒签名，缺失或过大的缩略图回退原图；签名前验证配置域名、协议、端口及基础路径，拒绝越界路径、目录穿越、片段和已带下载凭证的 URL。
- 使用现有七牛 Java SDK 对完整 URL 签名，支持中文、空格及图片处理参数，签名后不再整体编码。原因：本地 `x-file-storage-core 2.2.1` 的七牛预签名实现不保留查询参数。
- 私有文本及已有预览会话共用受限签名下载逻辑，不依赖七牛是否为当前激活存储；下载异常不向响应或异常链暴露签名，失败及读取完成后清理临时文件。
- 保留现有 ticket 一次兑换、session 有效期和下载许可语义；四个预览凭证入口不记录请求日志，避免 ticket/session 进入日志；上传 Token 响应增加 `@LogNoRet`。
- 私有文件及预览凭证响应设置 `Cache-Control: private, no-store` 和 `Referrer-Policy: no-referrer`；分片下载重置响应时保留调用方的这两个响应头。
- 第 5 项提前开发部分：仅服务器系统配置中的 `logo`、`logoWithText`、`loginBg`、`wxminiapp` 指定文件可匿名读取原图/缩略图；同一文件的文本接口仍要求登录，客户端不能声明公共素材。
- 未修改线上配置、Bucket 权限、数据库结构或历史文件记录；用户已确认本批人工验证通过，相关代码及方案纳入本次 Git 提交。

### 文件范围

- `fa-base/.../admin/annotation/FileAccess.java`、`FileSaveController.java`、`FileSaveBiz.java`、`FilePreviewTicketBiz.java`。
- `fa-base/.../interceptor/UserAuthRestInterceptor.java`、`UserDeviceInterceptor.java`、`filter/RequestAgainFilter.java`。
- `fa-core/.../utils/FaFileUtils.java`：仅保留分片响应重置前的缓存及引用来源策略。
- 新增 `FileSaveAccessTest`、`PrivateFileAuthTest`、`FilePreviewAccessTest`，扩展 `RequestAgainFilterTest`；同步本方案进度。

### 自动验证

- `mvn -o -q -pl fa-base -am -DskipTests=false -Dtest=FileSaveAccessTest,PrivateFileAuthTest,FilePreviewAccessTest,UserAuthRestInterceptorTest,RequestAgainFilterTest -Dsurefire.failIfNoSpecifiedTests=false test`：25 个测试通过，无失败或跳过。
- 覆盖匿名拒绝、公开兼容、公共素材例外、登录过期/账号冻结/后台资格停用、设备校验、600 秒签名及 URL 编码、路径校验、缩略图回退、私有文本及临时文件清理、预览 Range、凭证失效及日志保护。
- `git diff --check` 通过。自动检查未运行全量测试、前端构建或浏览器；本批无前端改动，真实私有桶人工验证已由用户确认通过。

### 本批人工验证步骤

1. 使用独立私有测试 Bucket，配置正确的 AK/SK、域名及基础路径，在系统中保存“私有”。通过后端上传接口 `/api/base/admin/fileSave/upload` 上传图片、文本、视频，记录文件 ID；本批不使用尚未适配的七牛直传登记流程。
2. 携带正常登录的 `Authorization` 请求 `getFile/{ID}`、`getFilePreview/{ID}`，确认 302 的 `Location` 为带 `e`、`token` 的可访问地址，中文/空格文件名和缩略图正常；无缩略图时回退原图。文本通过 `getFileStr/{ID}` 返回正常内容。登录凭据只放请求头或现有 Cookie，不拼进 URL。
3. 不带登录态请求上述三个普通文件接口，应返回 HTTP 401；过期登录同样失败。登录页已配置 Logo、背景等可匿名展示，但对应 `getFileStr/{ID}` 仍返回 401；不能通过添加“公共资源”参数放行其他文件。
4. 登录后通过 `createPreviewTicket` 申请 ticket，再调用 `exchangePreviewTicket` 兑换并访问返回的预览/下载地址，确认 Range 请求正常；重复兑换同一 ticket、使用无效或过期 session 应失败。检查凭证请求不出现在请求日志，分片响应仍有 `no-store`。
5. 保存一个已生成的七牛签名地址，等待超过 10 分钟，确认该旧地址被七牛拒绝；有效登录重新请求普通文件接口应得到可用新签名。确认普通私有响应有 `no-store`，文件记录不含临时凭证。
6. 在测试环境切换当前激活存储为本地，保持七牛私有配置，确认已有七牛文件的下载和文本读取仍正常。另用公开测试 Bucket 验证公开模式及其他存储的原有访问。

用户已确认本批验证通过，第 2 项已更新为 `✅已完成`；第 5 项登录页公共素材例外已验证通过，整体保留 `🟡进行中`，后续完成剩余特殊入口后再整体验收。

## 9. 第三批开发与验证记录（2026-10-15，已验证通过）

第三批范围：第 4 项“七牛直传与文件登记”，形成上传成功 → 后端校验对象并登记 → 通知业务成功的完整流程。该批未涉及第 3 项通用展示及下载、第 5 项剩余特殊入口；后续进展见第 10 节，整体切换仍需完成剩余适配。

### 已开发内容

- `syncUrlQiniu` 复用已实现的域名、协议及基础路径校验，登记时拒绝外部域名、越界路径、处理参数及临时下载凭证；原始对象 URL 编码后稳定入库。
- 通过现有七牛 SDK 的 `BucketManager.stat` 读取配置 Bucket 中对象的存在性、真实大小和 MIME；不再匿名请求外链或信任前端自报大小。不存在对象、元数据读取失败、非法文件类型及数据库写入失败均不能返回登记成功。
- 按直传工具已有“原名_时间_ID.扩展名”规则恢复完整原名及文件 ID，支持下划线、中文、空格、无扩展名和零字节文件；修正登记目录的末尾分隔符。
- 上传凭证接口仍要求现有登录鉴权，增加 `accessType` 返回值，仅声明公开/私有，不新增密钥字段。
- `fetchUploadImgQiniu` 统一在七牛上传完成后调用登记接口；`UploadFileQiniu` 移除重复登记，文件表单值仍为文件 ID；登记失败统一触发错误回调。
- 私有图片表单值仍使用 URL 字符串契约，但改为包含已登记文件 ID 的稳定系统访问地址，不存短效七牛签名；公开模式及旧版凭证响应继续返回公开外链。
- 图片回显不再给系统文件接口追加七牛处理参数；公开图片继续沿用原缩放参数。两个组件在成功、失败后均恢复 loading，并保留有效错误消息。
- 当前仓库未发现业务页面实际使用这两个七牛专用组件；共享组件行为已用定向测试验证。本批不新增演示页面，不迁移历史业务字段中保存的裸外链，也不清理登记失败后已上传的云端对象。

### 文件范围

- `fa-base/.../admin/biz/FileSaveBiz.java`、`rest/FileSaveController.java`，新增 `FileSaveQiniuSyncTest.java`。
- `frontend/fa-ui/packages/ui/src/components/base-uploader/` 下的 `utils.ts`、`UploadFileQiniu.tsx`、`UploadImgQiniu.tsx`、`index.ts`，新增 `qiniu.test.mjs`。
- `frontend/fa-ui/packages/ui/src/services/base/admin/fileSave.ts`，及本方案进度。

### 自动验证

- `mvn -o -q -pl fa-base -am -DskipTests=false -Dtest=FileSaveQiniuSyncTest,FileSaveAccessTest -Dsurefire.failIfNoSpecifiedTests=false test`：15 个测试通过，无失败或跳过，覆盖登记流程及原有私有读取回归。
- `node frontend/fa-ui/packages/ui/src/components/base-uploader/qiniu.test.mjs`：6 个测试通过，覆盖登记与成功回调顺序、公开兼容、私有地址、登记失败反馈、表单值契约和 loading 恢复。
- 使用项目 TypeScript 5.9.2 和 UI 配置检查 5 个受影响源文件，无相关类型诊断；检查时仅在内存中将共享配置的 `ignoreDeprecations` 适配为 `5.0`，未修改仓库配置，也未将此检查作为全包 typecheck。
- 6 个受影响前端文件的 `biome lint` 无错误，保留组件原有的 2 条默认 React 导入未使用警告；相关文件 `git diff --check` 通过。
- 自动检查未启动浏览器、开发服务器或执行完整前端构建、全量测试；用户已确认本批人工验证通过，相关代码及方案纳入本次 Git 提交。

### 本批人工验证步骤

1. 使用独立私有测试 Bucket，保存正确的七牛配置及“私有”访问类型；登录后获取 `getQiniuUploadToken`，确认含 `accessType: PRIVATE`，匿名请求仍被拒绝。
2. 使用七牛 SDK 或已接入专用上传组件的测试客户端直传一个含中文、空格、下划线的文件。对象 key 沿用 `{basePath}{目录}/{原名}_{14位时间}_{getWorkerId返回值}.{扩展名}`，成功后通过登录态调用 `syncUrlQiniu`。
3. 检查附件记录：大小与云端一致，MIME 正确，原始文件名未被第一个下划线截断，目录及文件 ID 正确，URL 不含 `e`、`token`；使用已完成的文件读取接口确认可访问。
4. 分别登记不存在对象、外部域名、越界路径和带下载签名的 URL，应明确失败且不新增附件记录。在消费页面验证登记失败不触发业务成功，两个上传组件均结束 loading。
5. 将测试 Bucket 与系统配置对应切换为公开，确认登记仍成功、公开外链回调保持兼容；私有模式下，新图片值应为系统文件接口地址、普通文件值仍为文件 ID。未接入消费页面时，组件回调及 loading 以已通过的前端行为测试为准。

用户已确认本批验证通过，第 4 项已更新为 `✅已完成`；其他功能保持原进度。

## 10. 第四批开发与验证记录（2026-10-16，已验证通过）

本批范围：完成第 5 项剩余的 OnlyOffice、应用发布下载及内部文件 URL 返回值适配。用户已确认本批验证通过，与此前已验证的登录页公共素材例外合并后，第 5 项整体已完成；第 3、6 项尚未完成，不直接整体切换生产私有访问。

### 已开发内容

- `FileSaveBiz.getFileUrl` 各入口及远程 `getFileFullPath` 对私有七牛文件复用已有 600 秒签名及地址校验，不依赖 HTTP 请求上下文；本地文件接口地址、实际磁盘路径及公开/其他远程存储 URL 的返回语义保持兼容。
- 内部文件 URL 方法不直接对外暴露，签名不写回附件实体；缺失附件返回明确错误。已核对实际调用方，目前外部 URL 消费入口为 OnlyOffice 和 `AppReleaseBiz` 的 APK/WGT 检查结果。
- OnlyOffice 文档配置在原有登录接口内生成授权访问地址，随后纳入现有 JWT；每次重新打开重新获取地址，不再固定拼接只能匿名访问的文件接口。回调地址及编辑模式沿用现有行为，不新增编辑保存逻辑。
- 应用发布 APK/WGT 结果通过已有 `getFileUrl` 调用取得短效地址；未改动发布、版本和设备范围判断，保留原有公开检查接口的匿名业务语义，不增加可按任意文件 ID 匿名取签名的端点。
- OnlyOffice 打开接口及应用 `check`、`checkApk`、`checkWgt` 响应设置 `Cache-Control: private, no-store`、`Referrer-Policy: no-referrer`；OnlyOffice 新增 `@LogNoRet`，应用检查保留已有 `@LogNoRet`，避免签名及包含签名的 JWT 进入响应日志。

### 文件范围

- `fa-base/.../admin/biz/FileSaveBiz.java`、`doc/biz/OnlyofficeBiz.java`、`doc/rest/OnlyofficeController.java`。
- `fa-base/.../FileSaveAccessTest.java`，新增 `doc/biz/OnlyofficeAccessTest.java`，及本方案进度。
- `fa-app/.../release/rest/AppReleaseController.java`，新增 `ReleaseDownloadAccessTest.java`、`ReleaseResponsePolicyTest.java`。

### 自动验证

- `mvn -o -q -pl fa-base -am -DskipTests=false -Dtest=FileSaveAccessTest,OnlyofficeAccessTest -Dsurefire.failIfNoSpecifiedTests=false test`：14 个测试通过，无失败或跳过。
- `fa-app` 不在当前根 POM 的模块清单中，单独运行时本机缓存的 `fa-base` 版本偏旧。使用任务临时目录中的聚合 POM，将工作区 `fa-core`、`fa-base`、`fa-app` 一起编译，以 `-pl :fa-app -am -DskipTests=false -Dtest=ReleaseDownloadAccessTest,ReleaseResponsePolicyTest,WgtCheckIsolationTest -Dsurefire.failIfNoSpecifiedTests=false test` 执行：6 个测试通过，无失败或跳过；未修改仓库 POM 或安装替换本机缓存依赖。
- 共 20 个定向测试覆盖私有签名、无 HTTP 上下文调用、公开和本地地址兼容、缺失文件、OnlyOffice 地址刷新及 JWT、响应缓存/日志策略、APK 版本条件、WGT 设备筛选与运行时/平台隔离。
- 相关文件 `git diff --check` 通过。未运行全量测试或前端构建，未启动浏览器、真实 OnlyOffice 文档服务或连接七牛 Bucket；本批没有前端改动。
- 用户已确认本批人工验证通过，相关代码及方案纳入本次 Git 提交；未修改线上配置、Bucket 权限、附件记录或其他任务的工作区改动。

### 本批人工验证步骤

1. 使用独立私有测试 Bucket 存放已登记的 Office 文档和测试发布包，配置对应的“私有”访问类型；使用现有登录入口打开文档，确认 `document.url` 为带 `e`、`token` 的可访问地址，文档服务不需要后台登录 Cookie 即可拉取。
2. 验证 OnlyOffice 首次加载、短时拉取重试及关闭后重新打开；检查配置中的文件 ID、标题、编辑模式和回调地址未改变。编辑保存继续使用业务原有流程，本批不补充基础模块中尚未实现的文件保存逻辑。
3. 调用应用 APK、WGT 检查接口，确认有合适已发布更新时返回可用的私有下载地址；无新版本或不符合 WGT 原有设备范围、渠道、运行时和平台条件时，不返回更新下载地址。原先允许匿名的检查请求仍可使用。
4. 检查文档配置和应用检查响应的 `no-store`、`no-referrer`，请求日志不含完整响应体、下载签名或配置 JWT；附件数据库 URL 不应包含临时凭证。
5. 保存一个签名地址，等待超过 10 分钟确认旧地址失效；重新打开文档或重新检查更新后，应得到新的可用地址。文档服务若在签名过期后才重新拉取原始资源，需要重新打开获取新配置，本批不提供后台自动续期。
6. 在公开测试 Bucket 和本地存储下回归相同入口：公开文件仍可访问，本地文档地址使用既有回调服务器，原有路径语义保持不变。

用户已确认本批验证通过，第 5 项已更新为 `✅已完成`；第 3、6 项保持原进度。


## 11. 第五批开发与验证记录（2026-10-16，已验证通过）

本批范围：将第 3 项拆分为 Web（3.1）和移动端（3.2），完成 Web 的同源访问及外部文档预览闭环。第 3.2、6 项仍待开发，不标记整体完成。

### 已开发内容

- 核对普通登录及 Token 换登录均调用现有 Sa-Token 登录流程；用真实 Sa-Token Servlet 上下文验证 `Authorization` Cookie 的 `Path=/`、30 天有效期、无请求头资源访问恢复登录及退出删除。管理端与 `@fa/ui` 的同源图片、缩略图、音视频、上传回显和下载继续使用稳定文件接口，不向 URL 添加长期登录 Token。
- `@fa/ui` 文件 service 新增 `getPreviewResource`，顺序申请一次性 ticket、兑换 600 秒 session；管理端复用该方法。每次调用重新申请，不缓存凭证；每个异步阶段检查登录及公司、租户上下文，取消请求或切换上下文后不再交付旧结果。
- `FaFileView` 及 `FilePreview` 的外部预览、外部降级入口统一使用 session 文件地址，保留文件名和水印；不再让 kkFileView 拉取需要浏览器 Cookie 的普通文件接口。
- 外部预览提供“重新加载预览”：凭证过期或加载失败后由当前登录态重新申请；卸载、切换文件及重新加载时取消旧请求、清除旧资源。iframe 设置 `no-referrer`，资源地址不持久化。
- `clearToken` 同步清除现有根路径登录 Cookie，避免 localStorage 清空后 `getToken` 又从 Cookie 恢复旧账号。Cookie 的签发和有效期仍由后端控制。
- 不更改接口权限、文件 ACL、数据库及七牛配置；本批不修改移动端，也不新增自动轮询刷新。

### 文件范围

- `frontend/fa-ui/packages/ui/src/services/base/admin/fileSave.ts`、`services/index.ts`、`utils/cache.ts`，新增 `fileSave.test.mjs`、`cache.test.mjs`。
- `frontend/apps/admin/features/fa-admin-pages/services/base/admin/fileSave.ts`，`components/file/FaFileView.tsx`、`FaFileUrlView.tsx`、`FilePreview.tsx`。
- `fa-base/src/test/java/com/faber/config/auth/FileResourceCookieTest.java` 及本方案。

### 自动验证

- `mvn -o -q -pl fa-base -am -DskipTests=false -Dtest=FileResourceCookieTest,PrivateFileAuthTest,FilePreviewAccessTest -Dsurefire.failIfNoSpecifiedTests=false test`：11 个测试通过，无失败或跳过，覆盖 Cookie 恢复、私有鉴权及预览会话 Range/失效/一次兑换回归。
- `node --test frontend/fa-ui/packages/ui/src/services/base/admin/fileSave.test.mjs frontend/fa-ui/packages/ui/src/utils/cache.test.mjs`：8 个测试通过，覆盖凭证流程、重新申请、登录与租户切换、取消、失败不回退匿名访问和 Cookie 清理。
- 使用项目 TypeScript 5.9.2 定向检查 7 个受影响源文件，无相关类型诊断；仅在内存适配 `ignoreDeprecations=5.0`，不替代全项目 typecheck。
- 受影响前端文件定向 `biome lint` 与本批文件 `git diff --check` 通过。未启动浏览器或开发服务器，未运行全量构建/测试。

### 本批人工验证步骤

1. 使用独立私有测试 Bucket，系统配置设为“私有”；重新登录管理端。检查 `Authorization` Cookie 的 Path 为 `/`，查看图片、头像、上传回显、原图预览、视频播放/拖动及文件下载，确认无需在 URL 中添加登录 Token。无痕访问普通私有文件接口仍应失败。
2. 在可被已配置 kkFileView 服务访问的测试环境，预览需要外部服务处理的文档（例如旧版 `.doc`）；也可从现有 `FaFileViewModal` 调用入口验证。确认先申请 ticket、兑换 session，kkFileView 的文件来源为 `getPreviewFile/{session}`，文件名含中文、空格时仍可显示，水印开关正常。浏览器原生图片、音视频及本地文档查看器继续走原有流程，不必触发外部降级。
3. 等待 session 超过 10 分钟，直接请求旧 session 确认失败；点击“重新加载预览”，确认生成新 session 并恢复预览。不要用 kkFileView 已缓存的转换页面判断 session 是否过期。
4. 在凭证申请期间关闭预览、切换文件，或退出并切换账号/租户，确认旧请求结果不覆盖新预览；退出后检查 localStorage 和根路径登录 Cookie 都已清除，普通私有文件入口无法继续凭旧登录访问。
5. 在公开测试 Bucket/公开配置及现有本地或 MinIO 环境，确认同源图片、下载和外部文档预览保持可用；检查保存的业务数据不含短效 session 或七牛签名。

注意：kkFileView 是受信任的外部文件处理服务，仍需能够访问应用地址；其转换缓存与服务自身访问控制由部署侧管理，应用 session 到期不会删除已被外部服务读取、缓存的内容。已签发 session 在有效期内继续遵守第 6 节的短效凭证限制，退出不会即时撤销。

用户已确认本批验证通过，第 3.1 项已更新为 `✅已完成`，本批相关代码及方案纳入 Git 提交；第 3 项整体保持 `🟡进行中`，移动端及整体验收保留原进度。


## 12. 第六批开发与验证记录（2026-10-16，已验证通过）

本批范围：第 3.2.1 项，完成普通 App 账号“上传 → 申请票据 → H5 预览或系统下载打开”的流程。图片、头像的直接文件地址仍待第 3.2.2 项适配，第 3、3.2 项整体保留 `🟡进行中`。

### 已开发内容

- 现有移动端上传、取票调用位于 `/api/base/admin`，正常登录但 `adminEnabled=false` 的 App 账号会被后台资格检查拒绝。新增 `/api/portal/fileSave/upload` 和 `/api/portal/fileSave/createPreviewTicket`，复用 `FileSaveBiz.upload` 和 `FilePreviewTicketBiz.createTicket`，不修改原有后台接口鉴权。
- 两个 Portal 入口不带匿名或设备豁免注解，沿用现有用户登录、账号状态、租户上下文及移动设备会话检查；冻结账号、失效会话、被撤销设备不能上传或签发新票据。
- 移动端 `uploadBaseFile`、`createFilePreviewTicket` 改用上述入口，调用签名及已有调用方保持兼容。H5 预览、浏览器/分享、语音和附件下载通过公共 API 自动采用新取票入口；系统下载、聊天附件缓存仍复用已有一次性 ticket/session、下载许可和页面/账号/工作空间隔离，不缓存临时地址，也不向资源 URL 添加登录 Token。
- Portal 取票路由加入现有凭证响应保护：在错误响应中也设置 `private, no-store`、`no-referrer`，且不写入请求日志。资源兑换和下载保持原有路由及有效期，不增加匿名任意文件 ID 签名能力。
- 同步移动端文件开发说明和定向检查；不改变文件记录、七牛权限、附件 ACL 或逐文件租户模型。

### 文件范围

- 新增 `fa-base/.../portal/file/rest/PortalFileSaveController.java`、`PortalFileSaveControllerTest.java`。
- `fa-base/.../filter/RequestAgainFilter.java`、`RequestAgainFilterTest.java` 及本方案。
- `mobile-x/features/fa-base-mobile/api/file.uts`、`tests/checks/base/check-files.mjs`、`docs/development/coding.md`。

### 自动验证

- 后端定向测试 `PortalFileSaveControllerTest`、`RequestAgainFilterTest`、`UserAuthRestInterceptorTest`、`FilePreviewAccessTest`：共 15 个测试通过，无失败或跳过。覆盖无后台资格账号的两个 Portal 入口、匿名/失效会话、账号冻结、设备拒绝、缺失文件及空文件 ID、凭证日志与缓存保护、原后台资格规则和预览会话回归。修正测试中缺失文件请求为 multipart 后，仅重跑失败测试所在类，其余已通过检查未重复。
- `cd mobile-x && pnpm check --group base --name check-files.mjs` 通过：验证新路由及登录/租户参数，回归独立票据、下载许可、系统打开、网络与打开失败、页面/会话切换、浏览器/分享及日志脱敏。
- 使用 HBuilderV 配套 SDK CLI `5.31.2026100911-alpha` 执行 Android `publish app-android --type appResource`，编译与资源导出成功；未新增原生模块，未构建完整 APK，未编译 iOS/小程序或运行真机。
- 本批文件 `git diff --check` 通过。未操作线上配置或测试 Bucket，未运行全量 Maven 测试。

### 本批人工验证步骤

1. 先部署支持两个 Portal 文件入口的后端，再运行本批客户端。使用独立私有测试 Bucket 和对应“私有”配置，以 `adminEnabled=false` 的正常 App 账号登录；配置可访问的 H5 预览服务。
2. 打开现有“文件上传与预览” Demo（调试模式下从 Demo 入口进入），选择图片上传。确认请求为 `/api/portal/fileSave/upload`，成功返回已登记文件 ID；点击“在线预览”，确认通过 Portal 取票并在 H5 显示图片。直接图片列表/头像回显不是本批验收范围。
3. 在同一 Demo 输入已登记的私有 PDF 或 Office 文件 ID，分别验证“在线预览”“系统打开文件”；后者使用目标系统支持的格式。确认下载 URL 只有短效 session、不含登录 Token；重复打开、预览页重新加载和外部浏览器打开分别申请新票据。
4. 下载或取票期间离开页面、退出/切换账号或工作空间，确认迟到结果不会打开旧文件或覆盖新页面；等待 session 超过 10 分钟后，旧链接应失败，重新操作可获得新会话。账号退出不会立即撤销已签发的有效 session。
5. 不带登录态、会话失效、账号冻结或设备信任被撤销时，确认两个 Portal 入口被拒绝；无后台资格账号仍不能调用原后台上传/取票接口。核对取票响应有 `no-store`，请求日志不包含 ticket/session。
6. 在公开测试 Bucket、现有本地或 MinIO 环境回归上传、在线预览与系统打开。后端应先于客户端发布，新客户端不向旧后台文件接口回退。

用户已确认本批验证通过，第 3.2.1 项已更新为 `✅已完成`，本批相关代码及方案纳入 Git 提交；第 3、3.2 项整体保持 `🟡进行中`，第 3.2.2、6 项仍待开发。
