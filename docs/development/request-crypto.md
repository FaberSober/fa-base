# 通用请求加密：Demo 与登录接入验证

Web、App Demo 及两个 `/loginV2`、管理端和 App 密码登录接入均已由用户确认验收通过。原登录接口保持不变。App 共享工具、原生基座与操作步骤见 `mobile-x/docs/development/request-crypto.md`。

## 开发环境直接演示

项目默认启用 `dev` profile，并开启加密功能和防重放。克隆后沿用现有数据库、Redis 配置启动后端，即可访问 Demo；macOS、Windows 不需要额外生成、复制私钥或设置用户目录。

- `application-dev.yml` 默认使用 `classpath:crypto/dev-public-request-key.pem`，密钥标识为 `dev-public-v1`。
- 文件位于 `fa-base/src/main/resources/crypto/dev-public-request-key.pem`，是**视为公开数据的开发测试资产**，仅用于假数据演示，不保护真实密码或客户数据。
- `FA_CRYPTO_PRIVATE_KEY_FILE`、`FA_CRYPTO_KID` 仍可覆盖开发默认值；本机已有外置密钥配置时优先使用该配置。
- classpath 密钥只允许在 `dev` 激活且 `prod` 未激活时加载；`prod`、`dev,prod` 或其他 profile 会拒绝这种加载方式。其他环境保持外置文件配置，不自动回退到开发密钥。

## 生产环境配置

生产部署显式启用 `prod` profile，并为客户生成独立的 RSA-2048 PKCS#8 私钥。例如在仓库外生成：

```sh
mkdir -p "$HOME/.config/fa-admin"
chmod 700 "$HOME/.config/fa-admin"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$HOME/.config/fa-admin/request-crypto.pem"
chmod 600 "$HOME/.config/fa-admin/request-crypto.pem"
```

让后端进程加载以下环境变量，路径替换为实际绝对路径：

```sh
export SPRING_PROFILES_ACTIVE=prod
export FA_CRYPTO_ENABLED=true
export FA_CRYPTO_KID=v1
export FA_CRYPTO_PRIVATE_KEY_FILE="$HOME/.config/fa-admin/request-crypto.pem"
export FA_CRYPTO_REPLAY_ENABLED=false
```

- Windows PowerShell 可用 `$env:SPRING_PROFILES_ACTIVE='prod'`、`$env:FA_CRYPTO_PRIVATE_KEY_FILE='C:\fa-config\request-crypto.pem'` 设置对应值；其他变量同理。
- 对应配置为 `fa.security.crypto.enabled`、`kid`、`private-key-file`、`replay.enabled`。
- `prod` 未指定私钥环境变量时，使用 `${user.home}/.config/fa-admin/request-crypto.pem`，其中 `user.home` 是运行 Java 进程的用户主目录；没有有效文件时启动失败，可设置 `FA_CRYPTO_ENABLED=false` 关闭此能力。以上示例显式关闭防重放，便于先验证关闭模式；正式开启时设为 `true`。
- 启用后缺少有效私钥会启动失败，错误信息不附带私钥内容。公钥从私钥推导，客户端不能获取私钥。
- 多实例必须使用相同配置与密钥；开启防重放时还需共用 Redis，沿用项目 Redis 前缀。
- 配置变更后重启服务。当前仅支持一个活动 `kid`，本批不提供轮换宽限期；更换密钥前先完成验收，轮换机制留待后续发布工作。
- 不提交真实生产私钥到仓库；内置公开开发密钥和自动化测试资源中的密钥均不能用于客户生产环境。
- 后端启动时沿用现有升级流程加载 `fa-demo` 的 MySQL / PostgreSQL `1.0.23_demo_crypto.sql` 菜单脚本，不增加业务表。

## 接口与使用约定

| 接口 | 约定 |
| --- | --- |
| `GET /api/base/security/crypto/public-key` | 返回协议版本、`kid`、SPKI DER Base64 公钥，可未登录访问 |
| `POST /api/base/security/crypto/prepare` | 当前仅支持 `{"purpose":"CRYPTO_DEMO"}`；要求登录并具有 `/admin/demo/crypto` 菜单权限 |
| `POST /api/base/security/crypto/prepare-login` | 匿名且许可异常时可用，仅支持 `ADMIN_LOGIN`、`PORTAL_LOGIN`；不接受 Demo 用途 |
| `POST /api/base/admin/auth/loginV2` | 匿名加密登录，固定 `ADMIN_LOGIN`；成功返回与旧管理端登录一致 |
| `POST /api/portal/auth/loginV2` | 匿名加密登录，固定 `PORTAL_LOGIN`；复用原 App 会话和设备信任逻辑 |
| `POST /api/base/demo/crypto/roundtrip` | 相同权限；仅接受加密信封，解密正文为 `{"text":"测试文本"}` |

Web 纯工具从 `@fa/core/crypto` 导出 `encryptRequest` 与相关类型，不依赖 UI。先获取公钥和当前准备信息，再传入正文与 `{purpose,method,path}` 生成信封；请求客户端负责发送，不自动重试或降级明文。

- 协议为 PLAN 中的 RSA-OAEP SHA-256 / MGF1 SHA-256 与 AES-256-GCM，标签拼接在密文尾部；每次生成新密钥及 12 字节 IV。
- AES 正文是 UTF-8 JSON，**JSON 总字节数**上限 4096（含字段名、引号及转义），不是 4096 个字符。
- 防重放关闭时，`challengeId`、`expiresAt` 为 `null`，不创建、读取或消费挑战；有效密文可以原样重放。
- 开启时挑战有效期 120 秒，绑定用途、路由、密钥及当前用户；成功解密且字段校验通过后，以 Redis `getAndDelete` 原子消费，仅一个并发请求能够通过。
- 密文篡改、附加字段、重复 JSON 字段、类型不符或额外 JSON 内容均拒绝；无效请求不消费有效挑战。
- Demo 不记录系统接口日志，不在遥测中主动记录正文或信封；页面不保存到本地存储。只使用普通测试文本。

## 人工验收

1. 配置启用功能，防重放设为 `false`，重启后台完成菜单升级；使用具备新菜单权限的账号登录。
2. 从“进阶功能 → 通用加密解密”进入，或访问 `/admin/demo/crypto`；使用 HTTPS 或 localhost。
3. 输入中文、Emoji、换行、空文本，点击“生成新密文”再点“提交后台解密”，确认结果与输入一致。F12 查看 `roundtrip` 请求时只有信封字段。相同文本重新生成，密文应不同。
4. 防重放关闭时，点击“原样重放”应成功；点击“篡改后提交”应失败，显示后台错误说明。
5. 设置 `FA_CRYPTO_REPLAY_ENABLED=true` 并重启，重新生成密文，确认页面显示开启。正常提交成功后，点击“原样重放”应失败。
6. 开启状态下重新生成密文，先点“篡改后提交”应失败，再点“提交后台解密”仍可成功；证明篡改不会提前消耗挑战。
7. 重新生成密文且不提交，等待显示的到期时间过后再提交，应被拒绝。
8. 使用无该菜单权限的账号调用准备/解密接口应被拒绝；未登录时也应被拒绝。F12 在信封外追加 `password` 等字段应被拒绝。

自动化覆盖 Web 原生加密、真实 Web→Java 样例互通、大小限制、篡改、严格字段校验、开关两种模式、并发消费与 Redis 异常，以及真实升级解析器读取两种 SQL 文件头。Redis 通过 mock 验证调用与并发语义，实际 Redis、数据库升级、浏览器页面与权限需按以上步骤验收。

本批已通过 16 项 Java 定向测试、5 项 Web 加密测试及 3 个修改文件的定向类型检查。核心测试可从仓库根目录执行（Node 测试使用支持直接加载 TypeScript 的 Node 版本，本次为 v26）：

```sh
node --test frontend/fa-ui/packages/core/tests/crypto.test.mjs
mvn -q -pl fa-demo -am -DskipTests=false -Dtest=RequestCryptoTest,RequestCryptoBizTest,RequestCryptoConfigurationTest,CryptoMenuSqlTest -Dsurefire.failIfNoSpecifiedTests=false test
```

## 登录接入与发布验收

管理端 `services/base/admin/auth.ts`、App `features/fa-base-mobile/api/auth.uts` 每次密码登录先获取公钥和登录准备信息，整体加密 `{username,password}` 后发送对应 `/loginV2`。密码不额外 trim 或归一化。短信、Token 登录、注册、Portal 网页及 Desktop 继续使用既有接口。

- 防重放开启时，一次挑战仅允许进入认证业务一次；密码错误也不恢复挑战。点击登录重试会重新准备和加密；网络失败不自动重发、不回退旧接口。
- 关闭防重放时仍要求合法信封，原样密文可以重新进入账号密码校验。直接提交明文或在信封外追加 `password` 等字段始终拒绝。
- 新加密登录及准备入口只在 `FA_CRYPTO_ENABLED=true` 时注册；关闭后新客户端明确失败，旧接口继续按原逻辑工作。客户端加密不能阻止用户自行调用旧明文接口，这是保留旧入口的兼容边界。
- 新登录入口使用 `FaNoLog`，不记录信封或解密后的账号密码、会话及设备信任凭据。App HTTP 日志沿用敏感字段脱敏。
- 先发布支持新旧接口的后端并启用有效密钥，再发布管理端和 App 资源；多实例统一密钥和防重放配置、共用 Redis。回滚客户端应配合旧版本资源，不依赖自动明文降级。
- App 使用上一批已包含 `fa-request-crypto` 的自定义基座；本批未改插件导出或宿主，无需新增原生依赖。仍在使用旧基座的设备需先重建安装，再同步页面。

本批自动验证：22 项 Java 定向测试（`RequestCryptoBizTest`、`RequestCryptoConfigurationTest`、`RequestCryptoLoginControllerTest`）、3 项管理端登录 service 测试、2 项 App 登录调用测试及 3 个受影响 TypeScript 文件类型检查通过。HBuilderV 配套 `5.31.2026100911-alpha` 两端资源编译通过，并确认产物含新登录调用；为避免同名工程混淆，编译使用指向当前源码的临时项目入口。Java 测试使用临时测试密钥和模拟 Redis/认证依赖，不能代替真实数据库、Redis、许可及设备运行验收。

人工验收请使用假数据测试账号，管理端、Android、iOS 分别执行：

1. 后台开启加密和防重放，发布最新管理端与 App 资源。正确密码登录成功，错误密码失败，随后改为正确密码应可登录；密码两侧空格应保留并按原密码规则校验。
2. 管理端 F12 保留网络日志，确认密码登录按 `public-key → prepare-login → loginV2` 执行，`loginV2` 请求体只有七个信封字段；使用“编辑并重发”重复同一已提交请求，应被拒绝，包括先前密码错误的请求。再次点击页面登录应重新申请挑战。
3. 从一次已提交请求复制信封，分别修改密文、在信封外追加 `password`，应拒绝；直接向两个 `/loginV2` 提交 `{username,password}` 也应拒绝。关闭防重放后重复该明文/篡改测试，仍应拒绝。
4. 设置 `FA_CRYPTO_REPLAY_ENABLED=false` 并重启，重新登录准备信息显示关闭，合法密文重发可再次进入密码校验；再恢复开启，确认重复请求拒绝。
5. App 登录后检查设备列表、退出再登录和已有设备信任行为；以旧版 App 或 Portal 网页验证原 `/api/portal/auth/login`，并验证原管理端 `/login`、Token/短信登录仍按原行为工作。
6. 在测试环境的许可异常状态验证公钥、`prepare-login` 和两个 `/loginV2` 可达，认证仍按账号规则执行；Demo 的 `prepare` 和解密权限保持原限制。不要改生产许可进行测试。

用户于 2026-10-16 确认本批验证通过，关联 PLAN 功能 4、5 已更新为 ✅已完成；未提供单独的设备、逐项测试或生产发布记录，不推定额外验证结论。
