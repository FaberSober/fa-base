# Linux 请求加密私钥生成与部署

创建时间：2026-10-17

## 适用范围

用于 Linux 生产环境配置请求加密私钥，对应 `fa-admin/src/main/resources/application-prod.yml`：

```yaml
fa:
  security:
    crypto:
      enabled: ${FA_CRYPTO_ENABLED:true}
      kid: ${FA_CRYPTO_KID:v1}
      private-key-file: ${FA_CRYPTO_PRIVATE_KEY_FILE:${user.home}/.config/fa-admin/request-crypto.pem}
```

后端要求 **RSA-2048、PKCS#8 格式、无口令的 PEM 私钥**。文件首行为 `-----BEGIN PRIVATE KEY-----`，后端从私钥自动推导并发布公钥，无需单独配置公钥文件。

`${user.home}` 是 Java 运行用户的主目录，可能与当前登录用户的主目录不同。建议通过 `FA_CRYPTO_PRIVATE_KEY_FILE` 指定绝对路径。生产环境必须启用 `prod`，不能使用仓库内的公开开发测试密钥。

## 1. 准备运行用户与 OpenSSL

以下生成命令使用 Bash，须以运行 Java 服务的 Linux 用户执行。例如服务用户为 `fa-admin` 时，先切换用户：

```bash
sudo -iu fa-admin
```

确认 OpenSSL 可用：

```bash
openssl version
```

未安装时，由管理员按发行版安装 `openssl` 包，再切回服务用户执行后续步骤。

## 2. 生成私钥

```bash
(
  set -e
  umask 077
  mkdir -p "$HOME/.config/fa-admin"
  chmod 700 "$HOME/.config/fa-admin"

  key_file="$HOME/.config/fa-admin/request-crypto.pem"
  # 禁止覆盖已有文件；已存在时命令会报错并退出。
  set -o noclobber
  openssl genpkey \
    -algorithm RSA \
    -pkeyopt rsa_keygen_bits:2048 > "$key_file"
  chmod 600 "$key_file"
)
```

此命令生成无口令的 PKCS#8 PEM 私钥，不要添加 `-aes256` 等口令加密参数，当前后端没有配置私钥解密口令的入口。

已有密钥时直接复用，不要每次部署都重新生成。若生成失败并留下空文件或不完整文件，应先确认该文件确实是本次失败生成的文件，再删除并重新生成。

## 3. 检查格式、有效性与权限

```bash
head -n 1 "$HOME/.config/fa-admin/request-crypto.pem"
openssl pkey \
  -in "$HOME/.config/fa-admin/request-crypto.pem" \
  -check -noout
ls -l "$HOME/.config/fa-admin/request-crypto.pem"
```

预期结果：

- 首行为 `-----BEGIN PRIVATE KEY-----`，不是 `BEGIN RSA PRIVATE KEY` 或 `BEGIN ENCRYPTED PRIVATE KEY`。
- OpenSSL 提示密钥有效，命令退出码为 `0`。
- 文件权限为 `-rw-------`，属主为 Java 服务运行用户；该用户能够访问上级目录。

不要用 `cat` 输出完整私钥，也不要将私钥提交到 Git、前端配置、日志或聊天中。

## 4. 配置并启动服务

### 终端启动

在生成密钥的同一个服务用户终端执行，将 JAR 路径替换为实际部署路径：

```bash
export FA_CRYPTO_PRIVATE_KEY_FILE="$HOME/.config/fa-admin/request-crypto.pem"
export FA_CRYPTO_KID="prod-v1"

java -jar /opt/fa-admin/fa-admin.jar --spring.profiles.active=prod
```

`export` 仅影响当前终端及其子进程。服务重启、自启动时应由部署配置持久设置环境变量。

### systemd 服务

由管理员创建 `/etc/fa-admin/crypto.env`（父目录不存在时先创建），填写实际绝对路径，例如：

```ini
FA_CRYPTO_PRIVATE_KEY_FILE=/home/fa-admin/.config/fa-admin/request-crypto.pem
FA_CRYPTO_KID=prod-v1
```

环境文件中不要使用 `$HOME`、`~` 或 `${user.home}`，systemd 不会按 Shell 方式展开这些值。环境文件只存路径和标识，不存私钥正文。

将环境文件权限设为 `600`，属主设为 `root`。通过 `sudo systemctl edit fa-admin` 在已有系统服务中添加：

```ini
[Service]
EnvironmentFile=/etc/fa-admin/crypto.env
```

确认原服务的 `User` 与私钥文件属主一致，且启动参数已启用 `prod`。然后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl restart fa-admin
sudo systemctl status fa-admin --no-pager
sudo journalctl -u fa-admin -n 50 --no-pager
```

示例服务名为 `fa-admin`，按实际名称替换。

### Docker 部署

在现有 Compose 服务中添加只读文件挂载与环境变量，例如：

```yaml
services:
  fa-admin:
    environment:
      SPRING_PROFILES_ACTIVE: prod
      FA_CRYPTO_PRIVATE_KEY_FILE: /run/secrets/request-crypto.pem
      FA_CRYPTO_KID: prod-v1
    volumes:
      - /home/fa-admin/.config/fa-admin/request-crypto.pem:/run/secrets/request-crypto.pem:ro
```

宿主机源文件必须提前生成，容器内运行 Java 的用户 UID/GID 需要具备读取权限。变量填写容器内路径，不是宿主机路径；不要将私钥打包进镜像。更新配置后重新创建对应容器。

## 5. 部署验收与排查

1. 确认应用使用 `prod` 配置启动，日志中没有“请求加密配置无效”错误。
2. 使用配套版本的管理端，在网络面板中确认登录准备接口成功返回，返回的 `kid` 与 `FA_CRYPTO_KID` 一致。
3. 使用测试账号确认登录成功；错误密码后重新输入正确密码仍可登录。
4. 若开启防重放，确认各实例使用同一 Redis；准备接口依赖 Redis 创建挑战。

| 现象 | 排查方向 |
| --- | --- |
| 启动提示请求加密配置无效 | 检查绝对路径、运行用户权限、文件格式、RSA 位数及 `kid`；`kid` 允许 1～64 位英文字母、数字、点、下划线和连字符 |
| 手工启动成功，systemd 启动失败 | 检查服务的 `User`、环境文件及目录访问权限；检查 unit 的文件系统访问限制 |
| 宿主机正常，容器启动失败 | 检查源文件是否存在、只读挂载目标、容器内路径与 UID/GID 权限 |
| 启动成功但登录准备失败 | 检查 Redis 连接、反向代理及客户端与后端版本是否配套 |

## 6. 备份、多实例与更换密钥

- 私钥生成一次后妥善备份，限制备份的访问权限，避免随应用发布覆盖或删除。
- 多实例使用同一份私钥和相同 `kid`，同步部署配置；开启防重放时共用 Redis。
- 更换私钥时同时更换 `kid`，例如 `prod-v1` 改为 `prod-v2`，避免客户端复用同 `kid` 缓存的旧公钥。
- 当前配置只加载一把活动私钥。建议在维护窗口暂停新的加密请求准备，等待旧挑战过期（当前默认 120 秒），再统一切换各实例。切换后客户端重新准备请求；不要让不同密钥的实例同时接收同一批请求。

协议与兼容细节见 [请求加密开发说明](../development/request-crypto.md)，整体方案见 [跨端通用加密与防重放方案](../plans/PLAN-base-003-跨端通用加密与防重放方案-1015.md)。
