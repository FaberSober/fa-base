# 通用请求加密：Web Demo 接入与验证

本批完成 Web Demo 及后台公共能力。App、`/loginV2` 尚未接入，原登录接口保持不变。

## 启用配置

当前项目 `application.yml` 默认开启加密功能和防重放，启动前需准备有效私钥；尚未准备密钥时，可设置 `FA_CRYPTO_ENABLED=false` 关闭此能力。先在仓库外生成 RSA-2048 PKCS#8 私钥，例如：

```sh
mkdir -p "$HOME/.config/fa-admin"
chmod 700 "$HOME/.config/fa-admin"
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$HOME/.config/fa-admin/request-crypto.pem"
chmod 600 "$HOME/.config/fa-admin/request-crypto.pem"
```

让后端进程加载以下环境变量，路径替换为实际绝对路径：

```sh
export FA_CRYPTO_ENABLED=true
export FA_CRYPTO_KID=v1
export FA_CRYPTO_PRIVATE_KEY_FILE="$HOME/.config/fa-admin/request-crypto.pem"
export FA_CRYPTO_REPLAY_ENABLED=false
```

- 对应配置为 `fa.security.crypto.enabled`、`kid`、`private-key-file`、`replay.enabled`。
- 未指定私钥环境变量时，项目使用 `${user.home}/.config/fa-admin/request-crypto.pem`，其中 `user.home` 是运行 Java 进程的用户主目录。以上示例显式关闭防重放，便于先验证关闭模式；验证开启模式时设为 `true`。
- 启用后缺少有效私钥会启动失败，错误信息不附带私钥内容。公钥从私钥推导，客户端不能获取私钥。
- 多实例必须使用相同配置与密钥；开启防重放时还需共用 Redis，沿用项目 Redis 前缀。
- 配置变更后重启服务。当前仅支持一个活动 `kid`，本批不提供轮换宽限期；更换密钥前先完成验收，轮换机制留待后续发布工作。
- 不提交真实私钥到仓库；测试资源中的密钥只用于自动化测试，不能用于部署。
- 后端启动时沿用现有升级流程加载 `fa-demo` 的 MySQL / PostgreSQL `1.0.23_demo_crypto.sql` 菜单脚本，不增加业务表。

## 接口与使用约定

| 接口 | 约定 |
| --- | --- |
| `GET /api/base/security/crypto/public-key` | 返回协议版本、`kid`、SPKI DER Base64 公钥，可未登录访问 |
| `POST /api/base/security/crypto/prepare` | 当前仅支持 `{"purpose":"CRYPTO_DEMO"}`；要求登录并具有 `/admin/demo/crypto` 菜单权限 |
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
