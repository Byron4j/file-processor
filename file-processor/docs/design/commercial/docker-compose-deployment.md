# Docker Compose 商用部署方案

## 架构概览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Docker Network: filemaster-network                  │
│                                                                             │
│  ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────────────┐  │
│  │    Nginx    │────│  file-master│────│         file-worker             │  │
│  │    :80     │    │    :8080    │    │          :8081                  │  │
│  │    :443    │    │  (主API服务) │    │      (异步任务处理器)            │  │
│  └─────────────┘    └──────┬──────┘    └─────────────────────────────────┘  │
│                            │                                                │
│         ┌──────────────────┼──────────────────┐                             │
│         │                  │                  │                             │
│    ┌────┴────┐      ┌─────┴──────┐    ┌─────┴──────┐                       │
│    │  MySQL  │      │   Redis    │    │  RabbitMQ  │                       │
│    │  :3306  │      │   :6379    │    │   :5672    │                       │
│    └─────────┘      └────────────┘    └────────────┘                       │
│                                                                             │
│    ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐          │
│    │   MinIO    │  │Meilisearch │  │  ClamAV   │  │ Prometheus │          │
│    │  :9000    │  │   :7700    │  │  :3310    │  │   :9090    │          │
│    └────────────┘  └────────────┘  └────────────┘  └────────────┘          │
│                                                                             │
│    ┌─────────────────────────────────────────────────────────────────┐     │
│    │                         Grafana :3000                          │     │
│    │                    (监控仪表盘)                                 │     │
│    └─────────────────────────────────────────────────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

## docker-compose.yml

```yaml
version: '3.8'

services:
  # ============================================
  # 应用服务层
  # ============================================
  
  file-master:
    build:
      context: ../..
      dockerfile: Dockerfile
    container_name: filemaster-api
    restart: unless-stopped
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/filemaster?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
      - SPRING_DATASOURCE_USERNAME=root
      - SPRING_DATASOURCE_PASSWORD=${MYSQL_ROOT_PASSWORD:-filemaster123}
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - SPRING_REDIS_PASSWORD=${REDIS_PASSWORD:-}
      - SPRING_RABBITMQ_HOST=rabbitmq
      - SPRING_RABBITMQ_PORT=5672
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USER:-admin}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASS:-admin123}
      - MINIO_ENDPOINT=http://minio:9000
      - MINIO_ACCESS_KEY=${MINIO_ACCESS_KEY:-minioadmin}
      - MINIO_SECRET_KEY=${MINIO_SECRET_KEY:-minioadmin123}
      - MEILISEARCH_HOST=http://meilisearch:7700
      - MEILISEARCH_API_KEY=${MEILISEARCH_API_KEY:-masterKey}
      - CLAMAV_HOST=clamav
      - CLAMAV_PORT=3310
    ports:
      - "8080:8080"
    volumes:
      - file-storage:/app/uploads
      - file-outputs:/app/outputs
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
      rabbitmq:
        condition: service_healthy
      minio:
        condition: service_healthy
    networks:
      - filemaster-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  file-worker:
    build:
      context: ../..
      dockerfile: Dockerfile.worker
    container_name: filemaster-worker
    restart: unless-stopped
    environment:
      - WORKER_MODE=true
      - SPRING_RABBITMQ_HOST=rabbitmq
      - SPRING_RABBITMQ_PORT=5672
      - SPRING_RABBITMQ_USERNAME=${RABBITMQ_USER:-admin}
      - SPRING_RABBITMQ_PASSWORD=${RABBITMQ_PASS:-admin123}
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      - FFMPEG_PATH=/usr/bin/ffmpeg
      - TESSERACT_PATH=/usr/bin/tesseract
    volumes:
      - file-storage:/app/uploads
      - file-outputs:/app/outputs
    depends_on:
      - rabbitmq
      - redis
    networks:
      - filemaster-network
    deploy:
      replicas: 2

  file-web:
    image: nginx:alpine
    container_name: filemaster-web
    restart: unless-stopped
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - file-web:/usr/share/nginx/html
    depends_on:
      - file-master
    networks:
      - filemaster-network

  # ============================================
  # 数据存储层
  # ============================================

  mysql:
    image: mysql:8.0
    container_name: filemaster-mysql
    restart: unless-stopped
    environment:
      - MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-filemaster123}
      - MYSQL_DATABASE=filemaster
      - MYSQL_USER=${MYSQL_USER:-filemaster}
      - MYSQL_PASSWORD=${MYSQL_PASSWORD:-filemaster123}
      - TZ=Asia/Shanghai
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql
      - ./mysql/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
      - ./mysql/my.cnf:/etc/mysql/conf.d/my.cnf:ro
    networks:
      - filemaster-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${MYSQL_ROOT_PASSWORD:-filemaster123}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  redis:
    image: redis:7-alpine
    container_name: filemaster-redis
    restart: unless-stopped
    command: >
      sh -c 'if [ -n "${REDIS_PASSWORD}" ]; then
        redis-server --appendonly yes --requirepass "${REDIS_PASSWORD}";
      else
        redis-server --appendonly yes;
      fi'
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - filemaster-network
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  minio:
    image: minio/minio:latest
    container_name: filemaster-minio
    restart: unless-stopped
    command: server /data --console-address ":9001"
    environment:
      - MINIO_ROOT_USER=${MINIO_ACCESS_KEY:-minioadmin}
      - MINIO_ROOT_PASSWORD=${MINIO_SECRET_KEY:-minioadmin123}
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data
    networks:
      - filemaster-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:9000/minio/health/live"]
      interval: 30s
      timeout: 20s
      retries: 3

  meilisearch:
    image: getmeili/meilisearch:latest
    container_name: filemaster-search
    restart: unless-stopped
    environment:
      - MEILI_MASTER_KEY=${MEILISEARCH_API_KEY:-masterKey}
      - MEILI_HTTP_ADDR=0.0.0.0:7700
    ports:
      - "7700:7700"
    volumes:
      - meilisearch-data:/meili_data
    networks:
      - filemaster-network

  # ============================================
  # 消息队列层
  # ============================================

  rabbitmq:
    image: rabbitmq:3-management-alpine
    container_name: filemaster-rabbitmq
    restart: unless-stopped
    environment:
      - RABBITMQ_DEFAULT_USER=${RABBITMQ_USER:-admin}
      - RABBITMQ_DEFAULT_PASS=${RABBITMQ_PASS:-admin123}
    ports:
      - "5672:5672"
      - "15672:15672"
    volumes:
      - rabbitmq-data:/var/lib/rabbitmq
    networks:
      - filemaster-network
    healthcheck:
      test: rabbitmq-diagnostics -q ping
      interval: 30s
      timeout: 30s
      retries: 3

  # ============================================
  # 安全服务层
  # ============================================

  clamav:
    image: clamav/clamav:latest
    container_name: filemaster-clamav
    restart: unless-stopped
    ports:
      - "3310:3310"
    volumes:
      - clamav-virusdb:/var/lib/clamav
    networks:
      - filemaster-network

  # ============================================
  # 监控运维层
  # ============================================

  prometheus:
    image: prom/prometheus:latest
    container_name: filemaster-prometheus
    restart: unless-stopped
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - prometheus-data:/prometheus
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'
      - '--web.console.libraries=/etc/prometheus/console_libraries'
      - '--web.console.templates=/etc/prometheus/consoles'
      - '--storage.tsdb.retention.time=15d'
      - '--web.enable-lifecycle'
    networks:
      - filemaster-network

  grafana:
    image: grafana/grafana:latest
    container_name: filemaster-grafana
    restart: unless-stopped
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=${GRAFANA_USER:-admin}
      - GF_SECURITY_ADMIN_PASSWORD=${GRAFANA_PASS:-admin123}
      - GF_USERS_ALLOW_SIGN_UP=false
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards:ro
      - ./grafana/datasources:/etc/grafana/provisioning/datasources:ro
    depends_on:
      - prometheus
    networks:
      - filemaster-network

  # Elasticsearch for centralized logging
  elasticsearch:
    image: elasticsearch:8.11.0
    container_name: filemaster-elasticsearch
    restart: unless-stopped
    environment:
      - discovery.type=single-node
      - xpack.security.enabled=false
      - "ES_JAVA_OPTS=-Xms512m -Xmx512m"
    ports:
      - "9200:9200"
    volumes:
      - elasticsearch-data:/usr/share/elasticsearch/data
    networks:
      - filemaster-network

  logstash:
    image: logstash:8.11.0
    container_name: filemaster-logstash
    restart: unless-stopped
    volumes:
      - ./logstash/logstash.conf:/usr/share/logstash/pipeline/logstash.conf:ro
    ports:
      - "5044:5044"
    depends_on:
      - elasticsearch
    networks:
      - filemaster-network

  kibana:
    image: kibana:8.11.0
    container_name: filemaster-kibana
    restart: unless-stopped
    ports:
      - "5601:5601"
    environment:
      - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
    depends_on:
      - elasticsearch
    networks:
      - filemaster-network

# ============================================
# 网络和卷
# ============================================

networks:
  filemaster-network:
    driver: bridge
    ipam:
      config:
        - subnet: 172.20.0.0/16

volumes:
  mysql-data:
  redis-data:
  minio-data:
  meilisearch-data:
  rabbitmq-data:
  clamav-virusdb:
  prometheus-data:
  grafana-data:
  elasticsearch-data:
  file-storage:
  file-outputs:
  file-web:
```

## 环境变量配置 (.env)

```bash
# Database
MYSQL_ROOT_PASSWORD=your_secure_root_password
MYSQL_USER=filemaster
MYSQL_PASSWORD=your_secure_db_password

# Redis
REDIS_PASSWORD=your_redis_password

# MinIO
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=your_secure_minio_password

# RabbitMQ
RABBITMQ_USER=admin
RABBITMQ_PASS=your_secure_rabbit_password

# Meilisearch
MEILISEARCH_API_KEY=your_master_key

# Grafana
GRAFANA_USER=admin
GRAFANA_PASS=your_grafana_password

# AI API Keys
CLAUDE_API_KEY=sk-ant-...
OPENAI_API_KEY=sk-...
```

## 启动步骤

```bash
# 1. 克隆代码
git clone <repo-url>
cd file-processor

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env 文件设置密码

# 3. 创建必要目录
mkdir -p docker/{nginx,grafana,prometheus,mysql,logstash}

# 4. 启动基础设施
make infra-up

# 5. 等待数据库初始化后启动应用
make app-up

# 6. 查看状态
make status
```

## 常用命令

```bash
# 完整启动
docker-compose up -d

# 仅启动基础设施
docker-compose up -d mysql redis minio rabbitmq meilisearch

# 查看日志
docker-compose logs -f file-master
docker-compose logs -f file-worker

# 扩容 worker
docker-compose up -d --scale file-worker=3

# 重启服务
docker-compose restart file-master

# 停止所有
docker-compose down

# 停止并清理数据
docker-compose down -v
```

## 端口映射

| 服务 | 端口 | 说明 |
|------|------|------|
| Nginx | 80, 443 | Web 入口 |
| FileMaster API | 8080 | REST API |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |
| MinIO | 9000, 9001 | 对象存储 + 控制台 |
| RabbitMQ | 5672, 15672 | 消息队列 + 管理界面 |
| Meilisearch | 7700 | 搜索引擎 |
| ClamAV | 3310 | 病毒扫描 |
| Prometheus | 9090 | 监控采集 |
| Grafana | 3000 | 监控仪表盘 |
| Kibana | 5601 | 日志查询 |
