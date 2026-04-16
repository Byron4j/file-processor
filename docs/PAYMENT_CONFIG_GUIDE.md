# 支付配置指南

本文档介绍如何配置 FileMaster Pro 的支付功能，包括支付宝和微信支付的商户信息配置。

---

## 目录

1. [支付宝配置](#支付宝配置)
2. [微信支付配置](#微信支付配置)
3. [环境变量配置](#环境变量配置)
4. [SSL/HTTPS配置](#sslhttps配置)
5. [测试与验证](#测试与验证)

---

## 支付宝配置

### 1. 注册支付宝开放平台账号

1. 访问 [支付宝开放平台](https://open.alipay.com/)
2. 使用企业支付宝账号登录
3. 完成企业实名认证

### 2. 创建应用

1. 进入"控制台" → "开发设置"
2. 点击"创建应用" → "网页/移动应用"
3. 填写应用名称、应用类型等信息
4. 提交审核（审核通过后可以使用沙箱环境测试）

### 3. 配置应用

#### 3.1 添加能力
- 进入应用详情页
- 点击"添加能力"
- 添加"电脑网站支付"能力

#### 3.2 配置密钥
1. 进入"开发设置" → "接口加签方式"
2. 选择"公钥"模式（推荐）
3. 使用 [支付宝密钥生成工具](https://opendocs.alipay.com/common/02khjo) 生成密钥对
4. 将生成的应用公钥填入支付宝开放平台
5. 保存支付宝返回的支付宝公钥
6. 保存生成的应用私钥

#### 3.3 配置应用网关和授权回调地址
- **应用网关**: 用于接收支付宝异步通知（可不填）
- **授权回调地址**: 用于授权回调（可不填）

### 4. 获取配置信息

在应用详情页获取以下信息：
- **APPID**: 应用ID
- **支付宝公钥**: 用于验证支付宝签名
- **应用私钥**: 用于生成请求签名

### 5. 沙箱环境测试

在正式上线前，建议先在沙箱环境测试：
1. 进入"沙箱" → "沙箱环境"
2. 使用沙箱环境的 APPID、公钥、私钥
3. 沙箱网关地址: `https://openapi.alipaydev.com/gateway.do`
4. 使用 [沙箱账号](https://openhome.alipay.com/platform/appDaily.htm?tab=account) 登录测试

---

## 微信支付配置

### 1. 注册微信支付商户号

1. 访问 [微信支付商户平台](https://pay.weixin.qq.com/)
2. 点击"成为商户"，按指引提交企业资料
3. 等待审核通过（通常1-5个工作日）
4. 审核通过后会获得商户号（mch_id）

### 2. 配置API v3密钥

1. 登录微信支付商户平台
2. 进入"账户中心" → "API安全"
3. 在"APIv3密钥"处点击"设置密钥"
4. 使用随机密码生成器生成32位随机密钥
5. **重要**: 密钥只显示一次，请务必妥善保存

### 3. 申请API证书

1. 进入"账户中心" → "API安全"
2. 在"API证书"处点击"申请证书"
3. 按指引完成证书申请流程
4. 下载证书压缩包，解压后包含：
   - `apiclient_cert.pem`: 商户证书
   - `apiclient_key.pem`: 商户证书私钥（**重要**）
   - `apiclient_serial_no`: 证书序列号

### 4. 配置Native支付

1. 进入"产品中心" → "我的产品"
2. 找到"Native支付"，点击开通
3. 配置Native支付的相关信息

### 5. 配置回调地址

1. 进入"产品中心" → "开发配置"
2. 在"支付配置" → "Native支付回调链接"处配置：
   - 格式: `https://your-domain.com/api/payment/wechat/notify`
   - 必须使用HTTPS
   - 域名需要ICP备案

---

## 环境变量配置

### 生产环境环境变量

建议在生产环境使用环境变量方式配置敏感信息：

```bash
# 数据库配置
export MYSQL_HOST=your-mysql-host
export MYSQL_PORT=3306
export MYSQL_DB=filemaster
export MYSQL_USER=filemaster
export MYSQL_PASSWORD=your-secure-password

# Redis配置
export REDIS_HOST=your-redis-host
export REDIS_PORT=6379
export REDIS_PASSWORD=your-redis-password

# 支付宝配置
export ALIPAY_APP_ID=2024XXXXXX
export ALIPAY_PRIVATE_KEY=MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQC...
export ALIPAY_PUBLIC_KEY=MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
export ALIPAY_SERVER_URL=https://openapi.alipay.com/gateway.do
export ALIPAY_NOTIFY_URL=https://your-domain.com/api/payment/alipay/notify
export ALIPAY_RETURN_URL=https://your-domain.com/payment/success

# 微信支付配置
export WECHAT_MCH_ID=1234567890
export WECHAT_APP_ID=wx1234567890abcdef
export WECHAT_API_V3_KEY=Your32CharacterLongKeyHere
export WECHAT_MCH_SERIAL_NO=34位十六进制证书序列号
export WECHAT_PRIVATE_KEY_PATH=/etc/filemaster/certs/apiclient_key.pem
export WECHAT_NOTIFY_URL=https://your-domain.com/api/payment/wechat/notify
```

### Docker Compose 配置示例

```yaml
version: '3.8'

services:
  filemaster-backend:
    image: filemaster/file-processor:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=mysql
      - MYSQL_PASSWORD=${MYSQL_PASSWORD}
      - REDIS_HOST=redis
      - ALIPAY_APP_ID=${ALIPAY_APP_ID}
      - ALIPAY_PRIVATE_KEY=${ALIPAY_PRIVATE_KEY}
      - ALIPAY_PUBLIC_KEY=${ALIPAY_PUBLIC_KEY}
      - ALIPAY_NOTIFY_URL=${ALIPAY_NOTIFY_URL}
      - WECHAT_MCH_ID=${WECHAT_MCH_ID}
      - WECHAT_APP_ID=${WECHAT_APP_ID}
      - WECHAT_API_V3_KEY=${WECHAT_API_V3_KEY}
      - WECHAT_MCH_SERIAL_NO=${WECHAT_MCH_SERIAL_NO}
    volumes:
      - ./certs/apiclient_key.pem:/etc/filemaster/certs/apiclient_key.pem:ro
    ports:
      - "8080:8080"
```

---

## SSL/HTTPS配置

### 为什么需要HTTPS

支付宝和微信支付都要求回调地址必须使用HTTPS：
- **支付宝**: 强制要求HTTPS，否则无法接收异步通知
- **微信支付**: API v3 强制要求HTTPS，且需要TLS 1.2+

### SSL证书申请

#### 方式1: Let's Encrypt（免费）

```bash
# 安装Certbot
sudo apt-get install certbot python3-certbot-nginx

# 申请证书
sudo certbot --nginx -d your-domain.com

# 自动续期
sudo certbot renew --dry-run
```

#### 方式2: 商业证书

从阿里云、腾讯云等平台购买商业SSL证书。

### Nginx配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    location /api/payment/alipay/notify {
        proxy_pass http://localhost:8080/api/payment/alipay/notify;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /api/payment/wechat/notify {
        proxy_pass http://localhost:8080/api/payment/wechat/notify;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 测试与验证

### 1. 支付宝测试

#### 沙箱环境测试
1. 配置沙箱环境的APPID和密钥
2. 访问 [沙箱账号页面](https://openhome.alipay.com/platform/appDaily.htm?tab=account)
3. 使用买家账号登录测试
4. 使用沙箱版支付宝APP扫码支付

#### 常见问题
- **问题**: "无效的APPID参数"
  - 解决: 检查APPID是否正确，是否使用的是沙箱APPID
- **问题**: "验签失败"
  - 解决: 检查公钥和私钥是否匹配，是否有多余的空格或换行

### 2. 微信支付测试

#### 测试流程
1. 配置测试环境的商户信息
2. 发起支付请求，获取支付二维码
3. 使用微信扫码支付
4. 检查订单状态是否更新

#### 常见问题
- **问题**: "商户号mch_id与appid不匹配"
  - 解决: 检查商户号是否与APPID绑定
- **问题**: "签名错误"
  - 解决: 检查API v3密钥是否正确，证书是否过期

### 3. 回调测试

#### 检查回调是否正常工作

```bash
# 检查支付宝回调
curl -X POST https://your-domain.com/api/payment/alipay/notify \
  -d "out_trade_no=FM20240101120000A1B2C3" \
  -d "trade_status=TRADE_SUCCESS"

# 应该返回: success
```

#### 查看日志

```bash
# 查看支付相关日志
tail -f /var/log/filemaster/application.log | grep -i payment

# 查看支付宝回调日志
tail -f /var/log/filemaster/application.log | grep -i alipay

# 查看微信支付日志
tail -f /var/log/filemaster/application.log | grep -i wechat
```

---

## 安全注意事项

1. **密钥保护**
   - 永远不要将私钥提交到代码仓库
   - 使用环境变量或密钥管理服务（如AWS KMS、阿里云KMS）
   - 定期轮换密钥

2. **HTTPS强制**
   - 生产环境必须使用HTTPS
   - 使用TLS 1.2或更高版本
   - 配置HSTS头

3. **回调验证**
   - 支付宝回调需要验证签名
   - 微信支付回调需要验证签名和解密
   - 实现幂等性处理，防止重复处理同一笔订单

4. **敏感信息脱敏**
   - 日志中不要输出完整的密钥信息
   - 数据库中敏感字段加密存储

---

## 相关文档

- [支付宝开放平台文档](https://opendocs.alipay.com/)
- [微信支付开发文档](https://pay.weixin.qq.com/wiki/doc/apiv3/index.shtml)
- [SSL证书配置指南](./SSL证书配置指南.md)

---

**最后更新**: 2026-04-12
