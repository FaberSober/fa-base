# Linux 请求加密私钥生成

创建时间：2026-10-17

`FA_CRYPTO_PRIVATE_KEY_FILE` 配置的是**私钥文件的绝对路径**，不是私钥正文。

## 1. 生成私钥文件

新服务器安装好 OpenSSL 后，**以运行 Java 服务的同一 Linux 用户**，在 Bash 中执行：

```bash
(
  set -e
  umask 077
  mkdir -p "$HOME/.config/fa-admin"
  chmod 700 "$HOME/.config/fa-admin"
  set -o noclobber
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 > "$HOME/.config/fa-admin/request-crypto.pem"
)
```

生成的文件为 `$HOME/.config/fa-admin/request-crypto.pem`，格式是后端要求的 **RSA-2048、无口令 PKCS#8 PEM**，权限为 `600`。无需另行生成公钥。

私钥只需生成一次；命令会拒绝覆盖已有文件。若生成失败并留下空文件，确认是本次失败产生的文件后，再删除重试。

## 2. 检查是否生成成功

```bash
openssl pkey -in "$HOME/.config/fa-admin/request-crypto.pem" -check -noout
```

提示密钥有效且退出码为 `0` 即成功。

## 3. 确认配置路径

`application-prod.yml` 已默认读取 **Java 运行用户主目录**下的 `.config/fa-admin/request-crypto.pem`，按上述方式生成并使用 `prod` 配置启动时，无需额外设置变量。

如需显式配置 `FA_CRYPTO_PRIVATE_KEY_FILE`，在现有部署配置中填写实际绝对路径，例如（将 `/home/fa-admin` 替换为实际用户主目录）：

```ini
FA_CRYPTO_PRIVATE_KEY_FILE=/home/fa-admin/.config/fa-admin/request-crypto.pem
```

妥善备份私钥，不要提交到 Git 或发送私钥正文。
