# 通用字段加密与 Header 防重放

当前为字段协议 v2：JSON 保持原业务结构，仅指定字符串字段变成密文；挑战和密钥标识放 Header。旧 `/login` 保留，两个 `/loginV2` 只接受新协议，不回退明文。新增 Web 纯 HTTP 兼容，状态为 🔍验证中，等待客户内网验收。

## 请求格式与协议

```json
{"username":"account","password":"encryptedKey.iv.ciphertextAndTag"}
```

```http
FaCryptoVersion: 2
FaCryptoKid: dev-public-v1
FaCryptoChallengeId: 开启防重放时的43字符一次性挑战
```

- Header 和三个密文片段均有长度、格式校验。关闭防重放时不发送挑战 Header；缺失版本/密钥 Header、重复 Header、篡改、明文或旧信封均拒绝。
- 每个字段使用新的随机 AES-256-GCM 密钥、12 字节 IV、16 字节标签；RSA-2048 OAEP SHA-256 / MGF1 SHA-256 封装 AES 密钥。二进制为标准 Base64，不换行。字段原文直接使用 UTF-8 字符串，不加 JSON 引号、不 trim，最多 4096 字节。
- AAD 为紧凑 UTF-8 JSON `[2,kid,challengeId,purpose,method,path,field]`，方法/路径/字段由服务端策略确定。改挑战、字段、用途或路由不能让旧密文生效。
- 原样业务字段仍明文，例如 Demo 的 `{text:密文字符串,age:18}`；`age` 不受字段密文完整性保护，继续由业务校验。客户端能为新值生成合法密文，本能力提高直接改包门槛，不代替业务权限或 HTTPS。

## 公共请求层

Web 使用现有 `BaseZeroApi`，只声明字段，不手写准备或 Header：

```ts
login = (username: string, password: string) =>
  this.post('loginV2', { username, password }, { crypto: { fields: ['password'] } });
```

普通接口同样可使用 `{crypto:{fields:['mobile']}}`，但必须先在后台登记其策略并在业务入口解密。仅增加前端配置不能为未登记接口创建挑战。

- `@fa/ui` 的 `requestPost` / `requestPut` 及 JSON 下载入口接收 `FaRequestConfig`。仅支持 JSON 顶层字符串字段；数组、FormData、GET 等不支持的配置明确失败，不悄悄发送明文。
- 请求层按目标方法、路径选择登录或业务准备接口；检查服务端允许字段，取得本次挑战，再加密并合并 Header，最后计算既有 `timestamp/us/bs`。`bs` 对最终密文 JSON 计算，不对加密前对象计算。
- 公钥仅在内存缓存，`kid` 变化时更新，并发共享公钥读取；每次请求单独准备挑战。更换私钥时同时更换 `kid`；不将同一标识用于不同密钥材料。
- 不改调用方原始对象。业务 Loading 从准备前开始，所有失败、取消或完成路径清理；内部准备/公钥请求不再次触发加密。准备、加密或提交失败不自动重试、不回退明文。
- Demo 为展示原始请求而显式调用 `prepareCryptoRequest`，保存返回的 `body/headers/preparation`，提交/重放时原样发送，避免重新申请挑战掩盖重放测试。

App 对应实现位于 `mobile-x/features/fa-base-mobile/common/request.uts`，追加 `RequestOptions`（第八个参数），例如 `{crypto:{fields:['password']}}`。认证、工作空间、设备信任 Header 与会话变化检查保留；原生密码插件导出未变，无需新增原生依赖。详见 `mobile-x/docs/development/request-crypto.md`。

## 后台接入

| 接口 | 权限与行为 |
| --- | --- |
| `GET /api/base/security/crypto/public-key` | 匿名、许可异常时可用；返回 v2、kid 和 SPKI DER Base64 公钥 |
| `POST /api/base/security/crypto/prepare-login` | 匿名、许可异常时可用；仅接受登记的两个登录目标 |
| `POST /api/base/security/crypto/prepare` | 必须登录、满足许可及目标菜单权限；不接收登录目标 |
| 两个 `/loginV2` | 接收 `{username,password}`，只解密 password，然后复用原登录逻辑 |
| `POST /api/base/demo/crypto/roundtrip` | 演示权限不变；接收 `{text,age}`，仅解密 text，age 原样返回 |

两个准备接口的请求体均为 `{method:"POST",path:"/api/实际目标"}`。服务端返回 `{v,kid,purpose,fields,replayEnabled,challengeId,expiresAt}`；客户端不能自报权限、用途或关闭策略。接口路径不包含 Servlet context path，AAD 也不包括查询参数。

业务入口的主要改动如下，仍需保留原权限、参数和数据归属校验：

```java
body.setPassword(requestCrypto.decryptField(body.getPassword(), request, "password"));
return ok(authBiz.login(body));
```

- `RequestCryptoBiz.TARGETS` 集中登记用途、路径、方法、字段及菜单权限；当前登记管理端 password、App password、Demo text。新增已登录业务须配置其菜单权限，并在 Controller 沿用原认证与权限约束；不能借登录匿名入口申请业务挑战。
- 当前每个目标只登记一个加密字段。一次请求多字段场景需统一解密/校验后消费挑战，不能直接重复调用当前消费方法。
- 版本、kid、方法、路径、用途、字段及登录后用户绑定挑战；开启时默认 120 秒，Redis 原子 `getAndDelete` 仅允许一次进入业务。密码错误等业务失败也不恢复挑战，页面重试重新准备。
- 无效密文在消费前拒绝；Redis 不可用则拒绝，不用进程内状态兜底。防重放关闭时不创建/读取/消费挑战，但仍验证 GCM。
- 两个新登录入口和 Demo 使用 `FaNoLog`；App 日志对字段加密请求整体脱敏，Demo 解密响应也不入日志。正常登录继续返回原会话和设备信任凭据。

## 配置、发布与兼容

- Web 同时支持 HTTPS、localhost 和纯 HTTP 内网 IP。`crypto.subtle` 可用时沿用原生 Web Crypto；不可用时按需加载随前端资源发布的 `node-forge`，保持 AES-256-GCM、RSA-OAEP SHA-256 / MGF1 SHA-256、AAD 和密文格式一致，无需浏览器白名单或外网 CDN。
- HTTP 实现的 AES 密钥、IV 和 OAEP seed 均由浏览器 `crypto.getRandomValues` 生成；缺失或调用失败时拒绝请求，不能改用 `Math.random` 或明文。原生加密报错时直接失败，不切换实现重试。
- HTTP 兼容无需新增配置或调整后台协议，重新构建并发布共享 UI 与管理端资源即可；发布时一并上传动态分包，避免旧 HTML/JS 缓存与新资源混用。继续启用 `FA_CRYPTO_ENABLED` 并使用客户独立生产私钥，关闭该开关不能让当前前端自动改用旧登录接口。
- 应用层加密不替代 TLS。HTTP 页面、脚本、公钥和会话凭据仍可能被窃听或篡改；兼容 HTTP 只解决浏览器加密 API 的可用性限制。
- `FA_CRYPTO_ENABLED` 控制新能力，`FA_CRYPTO_REPLAY_ENABLED` 控制挑战，沿用 `application.yml`；当前默认启用加密、关闭防重放，可用环境变量覆盖。配置改动重启后生效，多实例保持一致并共用 Redis。
- `dev` 使用 `classpath:crypto/dev-public-request-key.pem`、`kid=dev-public-v1`，仅供假数据测试，视为公开数据。`FA_CRYPTO_PRIVATE_KEY_FILE`、`FA_CRYPTO_KID` 可覆盖；生产显式启用 `prod` 并使用客户独立外置 RSA-2048 PKCS#8 私钥。classpath 测试密钥在 `prod`、`dev,prod` 或其他环境拒绝加载。
- `prod` 默认外置路径为 `${user.home}/.config/fa-admin/request-crypto.pem`；可用环境变量指定 macOS/Windows 的绝对路径。不提交客户私钥到仓库，不能用开发公开密钥保护真实数据。
- 本次 `/loginV2`、准备与 Demo 协议一起升级，需后台、管理端、App 配套发布；不兼容上一批的整包 `/loginV2`。旧 `/login`、Portal 网页、Desktop、短信/Token/注册保持原行为。回滚配合旧版本资源，不依赖自动明文降级。
- 开发 CORS 配置已允许自定义 Header；生产跨域网关需放行 `FaCryptoVersion/FaCryptoKid/FaCryptoChallengeId`，反向代理需保留这些 Header。此处未执行生产网关修改或上线。
- 已有 `fa-request-crypto` 原生插件无需改导出；未安装上一批新基座的设备仍须先更新基座，再同步最新页面。

## 验证

HTTP 兼容本次验证（2026-10-26）：23 项 JS/TS 定向测试、6 项 Java `RequestCryptoTest` 测试通过；覆盖缺少 `subtle` 时的登录与旧信封、UTF-8/边界长度、两种防重放模式、随机化、AAD/密文篡改、安全随机源失效、签名/Header 与失败清理。两个 HTTP JS 密文样例已由现有 Java 代码解密。共享加密模块 TypeScript 检查、浏览器目标独立打包与无 Node 全局变量环境的加密检查、冻结锁文件离线校验通过。未执行完整前端构建或客户内网实测。

字段协议改造历史验证：27 项 Java、23 项 JS/TS/UTS 定向测试及 7 个 TypeScript 文件类型检查通过，Android / iOS 资源在 HBuilderV 配套 `5.31.2026100911-alpha` 编译通过；详细记录以 PLAN 对应批次为准。Java 测试用测试密钥和模拟 Redis/认证依赖；跨端样例包括实际 Web Crypto、Swift（macOS）及 Android Kotlin（JVM，测试用 Base64 适配），不能替代设备或真实基础设施验证。HBuilderV 资源编译使用指向当前源码的临时项目入口，避免同名工程混淆。

人工验收（Web、Android、iOS）：

1. 后台与客户端同步更新，使用假数据账号。Demo 输入中文、Emoji、换行与空文本，生成后确认 JSON 只有 `text` 密文字符串和明文 `age:18`；版本、kid、挑战在 Header 展示。提交后原文和 age 均正确，明暗主题正常。
2. 防重放开启：新生成后先篡改提交应失败，原始请求仍可成功；随后原样重放应失败。再生成且不提交，等到期后至少 10 秒（默认可等 130 秒）再首次提交，应拒绝。
3. 防重放关闭并重启：重新生成，挑战 Header 消失，提交和原样重放成功，篡改仍失败。结束后恢复所需配置。
4. 管理端、App 正确密码登录成功，错误密码失败，之后改正确密码可重试。F12/调试网络里 username 明文、password 为字符串密文，无整包信封；业务请求只有一次，重复已提交的挑战应拒绝（包括密码错误的请求）。
5. 分别删除/修改版本、kid、挑战 Header，或改密文、向 `/loginV2` 直接提交明文，均应拒绝；不能用新的挑战 Header 配旧密文。检查加载状态会恢复，失败不自动改用旧接口。
6. 连续请求确认准备接口每次调用，公钥同 kid 在当前运行会话中复用。测试环境更换 kid 和对应密钥后，应重新读取公钥；不要用同 kid 替换密钥材料。
7. App 退出再登录、设备信任和现有设备列表正常；旧 `/login`、Portal 网页及 Token/短信等既有入口保持兼容。无 Demo 菜单权限用户不能准备/解密 Demo；许可异常时两个新登录和公钥/登录准备保持原可达性。
8. 跨域环境确认预检和网关转发三项 Header。生产发布、Windows 运行及真机签名未由本次自动验证覆盖。

HTTP 内网验收：

1. 发布新前端资源，关闭先前配置的“不安全来源视为安全”浏览器白名单，清理站点缓存后用 `http://客户IP[:端口]` 访问；控制台 `crypto.subtle` 应仍不可用，登录不再提示必须使用 HTTPS。
2. 验证正确密码登录、错误密码提示及再次提交；F12 中 `/loginV2` 的 password 应为三个 Base64 片段，认证与设备信任行为正常。
3. 在加密 Demo 输入中文、Emoji、换行和空文本，生成后提交可还原，篡改后被拒绝；防重放启用时原样重放应被拒绝。

HTTP 兼容保持 🔍验证中，用户确认后完成验收；既有功能的验收状态以 PLAN 为准。
