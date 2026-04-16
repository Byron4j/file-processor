# FileMaster Pro - Production Deployment Guide

This directory contains production deployment configurations for FileMaster Pro with SSL/HTTPS support for payment callbacks.

## Directory Structure

```
deploy/
├── docker-compose.prod.yml   # Production Docker Compose configuration
├── .env.example              # Environment variables template
├── init-ssl.sh               # SSL certificate initialization script
├── nginx/
│   └── filemaster.conf       # Nginx configuration with payment callbacks
└── README.md                 # This file
```

## Prerequisites

1. **Server Requirements**
   - Docker 20.10+ and Docker Compose 2.0+
   - 4GB+ RAM, 2+ CPU cores
   - 50GB+ disk space
   - Domain name with DNS pointing to server
   - Ports 80 and 443 open

2. **Payment Provider Accounts**
   - Alipay Open Platform account with application
   - WeChat Pay merchant account with API v3 certificate

## Quick Start

### 1. Prepare Environment

```bash
cd deploy

# Copy environment template
cp .env.example .env

# Edit with your actual values
nano .env
```

Fill in all required values:
- Domain name
- Database passwords
- Alipay APP ID and keys
- WeChat MCH ID, APP ID, and API v3 key

### 2. Configure WeChat Pay Certificate

Place your WeChat Pay certificate file:

```bash
# Copy your apiclient_key.pem from WeChat Pay platform
cp /path/to/apiclient_key.pem certs/
```

### 3. Initialize SSL Certificates

```bash
# Make script executable
chmod +x init-ssl.sh

# Run initialization (requires sudo for certbot)
./init-ssl.sh
```

This will:
- Request SSL certificates from Let's Encrypt
- Configure Nginx with HTTPS
- Set up auto-renewal

### 4. Start Services

```bash
docker-compose -f docker-compose.prod.yml up -d
```

### 5. Verify Deployment

```bash
# Check all containers are running
docker-compose -f docker-compose.prod.yml ps

# Check backend logs
docker-compose -f docker-compose.prod.yml logs -f backend

# Test HTTPS
curl -I https://your-domain.com/api/file/health
```

## Configuration Details

### Nginx SSL Configuration

The Nginx configuration (`nginx/filemaster.conf`) includes:

- **TLS 1.2/1.3 only** - Required by payment providers
- **OCSP Stapling** - Faster SSL handshake
- **HSTS Header** - Forces HTTPS connections
- **Security Headers** - X-Frame-Options, XSS Protection
- **Payment Callback Optimization**:
  - Raw request body passthrough for signature verification
  - Separate access logs for payment callbacks
  - Request method restrictions

### Payment Callback URLs

After deployment, configure these URLs in your payment provider dashboards:

**Alipay:**
```
https://your-domain.com/api/payment/alipay/notify
```

**WeChat Pay:**
```
https://your-domain.com/api/payment/wechat/notify
```

### Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `DOMAIN` | Your domain name | `filemaster.example.com` |
| `ALIPAY_APP_ID` | Alipay application ID | `2024XXXXXXXXXXXX` |
| `ALIPAY_PRIVATE_KEY` | Your app private key (RSA2) | `-----BEGIN...` |
| `ALIPAY_PUBLIC_KEY` | Alipay public key | `-----BEGIN...` |
| `WECHAT_MCH_ID` | WeChat merchant ID | `1234567890` |
| `WECHAT_APP_ID` | WeChat app ID | `wx1234567890abcdef` |
| `WECHAT_API_V3_KEY` | API v3 encryption key | 32-character key |
| `WECHAT_MCH_SERIAL_NO` | Certificate serial number | 34-character hex |

## Maintenance

### SSL Certificate Renewal

Certificates auto-renew via the certbot container. To test:

```bash
docker-compose -f docker-compose.prod.yml run --rm certbot renew --dry-run
```

### Backup

```bash
# Backup certificates
tar czf backup-$(date +%Y%m%d).tar.gz certbot-data/

# Backup database
docker-compose -f docker-compose.prod.yml exec mysql mysqldump -u root -p filemaster > backup.sql
```

### Update Deployment

```bash
# Pull latest images
docker-compose -f docker-compose.prod.yml pull

# Recreate containers
docker-compose -f docker-compose.prod.yml up -d

# Clean up old images
docker image prune -f
```

### View Logs

```bash
# All services
docker-compose -f docker-compose.prod.yml logs -f

# Specific service
docker-compose -f docker-compose.prod.yml logs -f backend

# Payment callbacks
docker-compose -f docker-compose.prod.yml exec nginx tail -f /var/log/nginx/payment-callback.log
```

## Troubleshooting

### SSL Certificate Issues

```bash
# Check certificate status
docker-compose -f docker-compose.prod.yml run --rm certbot certificates

# Force renewal
docker-compose -f docker-compose.prod.yml run --rm certbot renew --force-renewal

# Regenerate certificates (delete old ones first)
rm -rf certbot-data/live/your-domain.com
./init-ssl.sh
```

### Payment Callback Failures

1. **Check Nginx logs:**
   ```bash
   docker-compose -f docker-compose.prod.yml exec nginx tail -f /var/log/nginx/alipay-notify.log
   docker-compose -f docker-compose.prod.yml exec nginx tail -f /var/log/nginx/wechat-notify.log
   ```

2. **Verify SSL configuration:**
   ```bash
   openssl s_client -connect your-domain.com:443 -servername your-domain.com
   ```

3. **Test callback endpoints:**
   ```bash
   curl -X POST https://your-domain.com/api/payment/alipay/notify \
     -d "out_trade_no=TEST" -d "trade_status=TRADE_SUCCESS"
   ```

### Database Connection Issues

```bash
# Check MySQL is running
docker-compose -f docker-compose.prod.yml ps mysql

# Connect to MySQL
docker-compose -f docker-compose.prod.yml exec mysql mysql -u filemaster -p
```

## Security Checklist

- [ ] SSL certificates are valid and auto-renew
- [ ] TLS 1.2+ only (no TLS 1.0/1.1)
- [ ] HSTS header enabled
- [ ] Payment callback URLs use HTTPS
- [ ] Database passwords are strong
- [ ] WeChat Pay certificate permissions are restricted
- [ ] .env file is not in version control
- [ ] Firewall allows only 80/443
- [ ] Regular backups configured

## Support

For more details:
- [SSL Certificate Configuration Guide](../docs/SSL证书配置指南.md)
- [Payment Configuration Guide](../docs/PAYMENT_CONFIG_GUIDE.md)

