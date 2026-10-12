# 浏览器 Web Push VAPID 密钥生成与配置

## 用途

Web Push 使用一对 VAPID 密钥：浏览器订阅时使用公钥，服务端发送推送时使用私钥。VAPID 密钥与浏览器订阅返回的 `p256dh`、`auth` 密钥不同。项目的订阅接口读取 `fa.push.webpush.vapid-public-key`，发送接口从服务端环境变量读取私钥。

## 生成密钥

安装 Node.js 后，在 PowerShell 中运行以下命令。脚本使用 Node.js 内置 `crypto`，无需安装依赖：

```powershell
$script = @'
const { createECDH } = require("node:crypto");

function toBase64Url(value) {
  return value.toString("base64").replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

const ecdh = createECDH("prime256v1");
ecdh.generateKeys();

console.log(JSON.stringify({
  publicKey: toBase64Url(ecdh.getPublicKey(undefined, "uncompressed")),
  privateKey: toBase64Url(ecdh.getPrivateKey()),
}, null, 2));
'@
$script | node -
```

输出中的 `publicKey` 和 `privateKey` 属于同一密钥对。妥善保存私钥，不要提交到 Git、写入前端代码或发给浏览器。

## 配置开发环境

项目开发配置位于 `fa-admin/src/main/resources/application-dev.yml`：

```yaml
fa:
  push:
    webpush:
      vapid-public-key: ${FA_PUSH_WEBPUSH_VAPID_PUBLIC_KEY:替换为生成的公钥}
```

可将生成的公钥放在 `FA_PUSH_WEBPUSH_VAPID_PUBLIC_KEY` 环境变量中，或替换开发配置中的默认公钥。发送推送还需设置 `FA_PUSH_WEBPUSH_VAPID_PRIVATE_KEY` 和 `FA_PUSH_WEBPUSH_VAPID_SUBJECT`。Windows PowerShell 当前终端设置示例：

```powershell
$env:FA_PUSH_WEBPUSH_VAPID_PUBLIC_KEY = "生成的公钥"
$env:FA_PUSH_WEBPUSH_VAPID_PRIVATE_KEY = "生成的私钥"
$env:FA_PUSH_WEBPUSH_VAPID_SUBJECT = "mailto:admin@example.com"
```

设置后重启使用 `dev` 配置的后端。部署环境应通过环境变量或密钥管理服务配置公钥、私钥和有效联系地址；不要把私钥加入仓库。更换公钥后，浏览器端需要重新订阅。
