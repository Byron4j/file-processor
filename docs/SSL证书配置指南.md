# FileMaster Pro SSL证书配置指南

## 概述

本文档介绍如何为 FileMaster Pro 配置生产环境 SSL/TLS 证书，确保数据传输安全。

---

## 一、证书类型选择

### 1.1 免费证书（推荐测试使用）

| 提供商 | 有效期 | 特点 |
|--------|--------|------|
| Let's Encrypt | 90天 | 自动续期，社区支持 |
| ZeroSSL | 90天 | 免费额度，易用 |

### 1.2 商业证书（推荐生产使用）

| 类型 | 验证级别 | 适用场景 | 价格区间 |
|------|----------|----------|----------|
| DV (Domain Validated) | 域名验证 | 个人/小微企业 | ¥200-500/年 |
| OV (Organization Validated) | 组织验证 | 企业官网 | ¥1000-3000/年 |
| EV (Extended Validation) | 扩展验证 | 金融/电商 | ¥3000+/年 |
| 通配符证书 | 域名验证 | 多子域名 | ¥2000-5000/年 |

### 1.3 国内证书供应商

- **阿里云 SSL**: https://www.aliyun.com/product/cas
- **腾讯云 SSL**: https://cloud.tencent.com/product/ssl
- **华为云 SSL**: https://www.huaweicloud.com/product/scm.html

---

## 二、Let's Encrypt 证书配置（推荐）

### 2.1 使用 Certbot 申请证书

```bash
# 安装 Certbot
sudo apt update
sudo apt install certbot

# 申请证书（Standalone 模式）
sudo certbot certonly --standalone -d your-domain.com -d www.your-domain.com

# 证书位置
# /etc/letsencrypt/live/your-domain.com/fullchain.pem
# /etc/letsencrypt/live/your-domain.com/privkey.pem
```

### 2.2 自动续期配置

```bash
# 测试自动续期
sudo certbot renew --dry-run

# 添加到定时任务（每周执行）
echo "0 3 * * 1 certbot renew --quiet" | sudo crontab -
```

### 2.3 Spring Boot 配置

```yaml
# application-prod.yml
server:
  port: 443
  ssl:
    enabled: true
    key-store-type: PEM
    key-store: file:/etc/letsencrypt/live/your-domain.com/fullchain.pem
    key-store-password: ""
    key-alias: tomcat
```

---

## 三、Nginx 反向代理 + SSL 配置（推荐生产架构）

### 3.1 架构图

```
用户 → Nginx (443/SSL) → Spring Boot (8080)
```

### 3.2 Nginx 配置

```nginx
# /etc/nginx/sites-available/filemaster

server {
    listen 80;
    server_name your-domain.com www.your-domain.com;

    # HTTP 重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl http2;
    server_name your-domain.com www.your-domain.com;

    # SSL 证书配置
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;

    # SSL 安全配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers off;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # OCSP Stapling
    ssl_stapling on;
    ssl_stapling_verify on;

    # HSTS (强制HTTPS)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

    # 安全响应头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # 日志配置
    access_log /var/log/nginx/filemaster-access.log;
    error_log /var/log/nginx/filemaster-error.log;

    # 前端静态资源
    location / {
        root /var/www/filemaster-web/dist;
        try_files $uri $uri/ /index.html;

        # Gzip 压缩
        gzip on;
        gzip_types text/plain text/css application/json application/javascript;
    }

    # API 代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_cache_bypass $http_upgrade;

        # 超时配置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
    }

    # WebSocket 代理
    location /ws {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 大文件上传配置
    client_max_body_size 5G;
    proxy_request_buffering off;
}
```

### 3.3 支付回调专用配置

支付回调对 SSL 和请求处理有特殊要求，建议单独配置：

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 配置（同上）
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;

    # 支付回调专用日志
    access_log /var/log/nginx/payment-callback.log;

    # 支付宝回调 - 需要原始请求体
    location /api/payment/alipay/notify {
        proxy_pass http://localhost:8080/api/payment/alipay/notify;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Content-Type $content_type;
        proxy_set_header Content-Length $content_length;

        # 禁用缓冲，确保原始请求体传递
        proxy_buffering off;
        proxy_request_buffering off;

        # 支付回调超时配置
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;

        # 限制请求体大小
        client_max_body_size 1M;

        # 仅允许 POST 请求
        limit_except POST {
            deny all;
        }
    }

    # 微信支付回调 - 需要原始 XML/JSON 请求体
    location /api/payment/wechat/notify {
        proxy_pass http://localhost:8080/api/payment/wechat/notify;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Content-Type $content_type;
        proxy_set_header Content-Length $content_length;

        # 禁用缓冲
        proxy_buffering off;
        proxy_request_buffering off;

        # 超时配置
        proxy_connect_timeout 30s;
        proxy_send_timeout 30s;
        proxy_read_timeout 30s;

        # 限制请求体大小
        client_max_body_size 1M;

        # 仅允许 POST 请求
        limit_except POST {
            deny all;
        }
    }

    # 其他 API 代理
    location /api {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

### 3.3 启用配置

```bash
# 创建符号链接
sudo ln -s /etc/nginx/sites-available/filemaster /etc/nginx/sites-enabled/

# 测试配置
sudo nginx -t

# 重启 Nginx
sudo systemctl restart nginx
```

---

## 四、Docker Compose 部署 SSL 配置

### 4.1 使用 Nginx + Certbot 容器

```yaml
# docker-compose.yml
version: '3.8'

services:
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./certbot/conf:/etc/letsencrypt
      - ./certbot/www:/var/www/certbot
      - ./frontend/dist:/var/www/html
    depends_on:
      - backend
    command: "/bin/sh -c 'while :; do sleep 6h & wait $${!}; nginx -s reload; done & nginx -g \"daemon off;\"'"

  certbot:
    image: certbot/certbot
    volumes:
      - ./certbot/conf:/etc/letsencrypt
      - ./certbot/www:/var/www/certbot
    entrypoint: "/bin/sh -c 'trap exit TERM; while :; do certbot renew; sleep 12h & wait $${!}; done;'"

  backend:
    image: filemaster-backend:latest
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SERVER_PORT=8080
    ports:
      - "8080:8080"
```

### 4.2 初始化证书脚本

```bash
#!/bin/bash
# init-ssl.sh

DOMAIN=${DOMAIN:-your-domain.com}
EMAIL=${EMAIL:-admin@your-domain.com}

docker-compose run --rm certbot certonly \
  --webroot \
  --webroot-path=/var/www/certbot \
  --email $EMAIL \
  --agree-tos \
  --no-eff-email \
  -d $DOMAIN \
  -d www.$DOMAIN
```

---

## 五、Spring Boot 应用层安全配置

### 5.1 强制 HTTPS 重定向

```java
@Configuration
public class HttpsConfig {

    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
            @Override
            protected void postProcessContext(Context context) {
                SecurityConstraint securityConstraint = new SecurityConstraint();
                securityConstraint.setUserConstraint("CONFIDENTIAL");
                SecurityCollection collection = new SecurityCollection();
                collection.addPattern("/*");
                securityConstraint.addCollection(collection);
                context.addConstraint(securityConstraint);
            }
        };
        tomcat.addAdditionalTomcatConnectors(redirectConnector());
        return tomcat;
    }

    private Connector redirectConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(80);
        connector.setSecure(false);
        connector.setRedirectPort(443);
        return connector;
    }
}
```

### 5.2 安全配置

```yaml
# application-prod.yml
server:
  forward-headers-strategy: native
  tomcat:
    remoteip:
      remote-ip-header: X-Forwarded-For
      protocol-header: X-Forwarded-Proto

# 支付回调需配置
payment:
  alipay:
    return-url: https://your-domain.com/payment/success
    notify-url: https://your-domain.com/api/payment/alipay/notify
  wechat:
    notify-url: https://your-domain.com/api/payment/wechat/notify
```

---

## 六、SSL 测试与验证

### 6.1 在线检测工具

- **SSL Labs**: https://www.ssllabs.com/ssltest/
- **SSL Checker**: https://www.sslchecker.com/
- **证书有效期检查**: https://certificate.transparency.dev/

### 6.2 命令行测试

```bash
# 检查证书信息
echo | openssl s_client -servername your-domain.com -connect your-domain.com:443 2>/dev/null | openssl x509 -noout -dates -subject -issuer

# 检查 SSL 配置
nmap --script ssl-enum-ciphers -p 443 your-domain.com

# 测试 HTTPS 响应
curl -I https://your-domain.com/api/file/health
```

### 6.3 预期评分

使用 SSL Labs 测试应达到 **A+** 评级：
- 证书有效且受信任
- 支持 TLS 1.2/1.3
- 禁用弱加密算法
- 启用 HSTS
- 证书透明

---

## 七、故障排除

### 7.1 证书过期

```bash
# 手动续期
sudo certbot renew --force-renewal

# 重启服务
sudo systemctl restart nginx
```

### 7.2 混合内容警告

确保所有资源使用 HTTPS：
```html
<!-- 错误 -->
<script src="http://example.com/script.js"></script>

<!-- 正确 -->
<script src="https://example.com/script.js"></script>
<!-- 或使用相对协议 -->
<script src="//example.com/script.js"></script>
```

### 7.3 支付回调失败

确保支付回调 URL 使用 HTTPS：
```yaml
payment:
  alipay:
    notify-url: https://your-domain.com/api/payment/alipay/notify
  wechat:
    notify-url: https://your-domain.com/api/payment/wechat/notify
```

**常见问题及解决:**

1. **支付宝回调提示 "验签失败"**
   - 原因：Nginx 修改了请求参数或编码
   - 解决：确保 `proxy_buffering off` 和 `proxy_request_buffering off`

2. **微信回调提示 "签名错误"**
   - 原因：请求体被 Nginx 缓冲，微信无法读取原始请求体
   - 解决：禁用请求体缓冲，设置 `proxy_set_header Content-Length $content_length`

3. **回调无法到达服务器**
   - 检查防火墙是否开放 443 端口
   - 确认域名解析正确
   - 查看 Nginx 访问日志 `/var/log/nginx/payment-callback.log`

**验证回调配置:**

```bash
# 测试支付宝回调 URL
curl -X POST https://your-domain.com/api/payment/alipay/notify \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "out_trade_no=TEST123" \
  -d "trade_status=TRADE_SUCCESS" \
  -v

# 预期响应: HTTP/1.1 200 OK，返回体: success

# 测试微信支付回调 URL
curl -X POST https://your-domain.com/api/payment/wechat/notify \
  -H "Content-Type: application/json" \
  -d '{"out_trade_no":"TEST123","trade_state":"SUCCESS"}' \
  -v

# 预期响应: HTTP/1.1 200 OK
```

---

## 八、监控与告警

### 8.1 证书过期监控

```bash
#!/bin/bash
# check-ssl-expiry.sh

DOMAIN="your-domain.com"
EXPIRY_DATE=$(echo | openssl s_client -servername $DOMAIN -connect $DOMAIN:443 2>/dev/null | openssl x509 -noout -dates | grep notAfter | cut -d= -f2)
EXPIRY_EPOCH=$(date -d "$EXPIRY_DATE" +%s)
CURRENT_EPOCH=$(date +%s)
DAYS_UNTIL_EXPIRY=$(( (EXPIRY_EPOCH - CURRENT_EPOCH) / 86400 ))

if [ $DAYS_UNTIL_EXPIRY -lt 7 ]; then
    echo "WARNING: SSL certificate expires in $DAYS_UNTIL_EXPIRY days"
    # 发送告警通知
fi
```

### 8.2 添加到定时任务

```bash
# 每天检查一次
0 9 * * * /path/to/check-ssl-expiry.sh >> /var/log/ssl-check.log 2>&1
```

---

## 九、参考文档

- [Let's Encrypt 官方文档](https://letsencrypt.org/docs/)
- [Certbot 文档](https://eff-certbot.readthedocs.io/)
- [Mozilla SSL Configuration Generator](https://ssl-config.mozilla.org/)
- [Spring Boot SSL 配置](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#web.servlet.embedded-container.configuration.ssl)

---

**文档版本**: 1.0  
**更新日期**: 2026/04/12
