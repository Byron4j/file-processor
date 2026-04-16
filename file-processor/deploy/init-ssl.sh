#!/bin/bash
# FileMaster Pro - SSL Certificate Initialization Script
# This script initializes SSL certificates using Let's Encrypt

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Load environment variables
if [ -f .env ]; then
    source .env
else
    echo -e "${RED}Error: .env file not found${NC}"
    echo "Please copy .env.example to .env and fill in your values"
    exit 1
fi

# Check required variables
if [ -z "$DOMAIN" ] || [ "$DOMAIN" = "your-domain.com" ]; then
    echo -e "${RED}Error: DOMAIN not set in .env file${NC}"
    exit 1
fi

if [ -z "$EMAIL" ] || [ "$EMAIL" = "admin@your-domain.com" ]; then
    echo -e "${RED}Error: EMAIL not set in .env file${NC}"
    exit 1
fi

echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN}FileMaster Pro SSL Initialization${NC}"
echo -e "${GREEN}=======================================${NC}"
echo "Domain: $DOMAIN"
echo "Email: $EMAIL"
echo ""

# Create necessary directories
echo -e "${YELLOW}Creating directories...${NC}"
mkdir -p certbot-data certbot-www nginx/ssl certs

# Check if certificates already exist
if [ -d "certbot-data/live/$DOMAIN" ]; then
    echo -e "${YELLOW}Certificates already exist for $DOMAIN${NC}"
    read -p "Do you want to renew/replace them? (y/N): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${GREEN}Using existing certificates${NC}"
        exit 0
    fi
fi

# Start nginx temporarily for webroot challenge
echo -e "${YELLOW}Starting temporary nginx for certificate validation...${NC}"
docker-compose -f docker-compose.prod.yml up -d nginx

# Wait for nginx to be ready
sleep 5

# Request certificate
echo -e "${YELLOW}Requesting SSL certificate from Let's Encrypt...${NC}"
docker-compose -f docker-compose.prod.yml run --rm certbot certonly \
    --webroot \
    --webroot-path=/var/www/certbot \
    --email "$EMAIL" \
    --agree-tos \
    --no-eff-email \
    -d "$DOMAIN" \
    -d "www.$DOMAIN" \
    || {
        echo -e "${RED}Certificate request failed${NC}"
        echo "Please check:"
        echo "1. Domain DNS is pointing to this server"
        echo "2. Port 80 is open and accessible"
        echo "3. Domain name is correct in .env file"
        docker-compose -f docker-compose.prod.yml down
        exit 1
    }

# Copy certificates to nginx ssl directory
echo -e "${YELLOW}Copying certificates...${NC}"
cp certbot-data/live/$DOMAIN/fullchain.pem nginx/ssl/
cp certbot-data/live/$DOMAIN/privkey.pem nginx/ssl/

# Update nginx config with actual domain
echo -e "${YELLOW}Updating nginx configuration...${NC}"
sed -i "s/server_name _;/server_name $DOMAIN www.$DOMAIN;/g" nginx/filemaster.conf
sed -i "s|ssl_certificate .*|ssl_certificate /etc/nginx/ssl/fullchain.pem;|g" nginx/filemaster.conf
sed -i "s|ssl_certificate_key .*|ssl_certificate_key /etc/nginx/ssl/privkey.pem;|g" nginx/filemaster.conf

# Restart nginx with SSL
echo -e "${YELLOW}Restarting nginx with SSL...${NC}"
docker-compose -f docker-compose.prod.yml restart nginx

echo ""
echo -e "${GREEN}=======================================${NC}"
echo -e "${GREEN}SSL Certificate initialized successfully!${NC}"
echo -e "${GREEN}=======================================${NC}"
echo ""
echo "Certificate location: certbot-data/live/$DOMAIN/"
echo "Expiry date:"
docker-compose -f docker-compose.prod.yml run --rm certbot certificates | grep "Expiry"
echo ""
echo -e "${YELLOW}Important:${NC}"
echo "1. Certificates will auto-renew via certbot container"
echo "2. Test renewal with: ./test-ssl.sh"
echo "3. Backup the certbot-data directory"
echo ""
