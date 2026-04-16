# Phase 10: 运维监控

## 目标
建立完整的监控、日志、告警体系，确保系统稳定运行和快速故障排查。

## 监控架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        监控数据采集层                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │Micrometer│  │Logback   │  │Tracing   │  │Health Indicator  │  │
│  │指标收集   │  │日志输出   │  │链路追踪   │  │健康检查           │  │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────────┬─────────┘  │
│       │             │             │                  │            │
│       ▼             ▼             ▼                  ▼            │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │                      数据存储层                             │  │
│  │  Prometheus (指标)  │  Elasticsearch (日志)  │  Jaeger    │  │
│  └────────────────────────────────────────────────────────────┘  │
│                              │                                    │
│       ┌──────────────────────┼──────────────────────┐             │
│       ▼                      ▼                      ▼             │
│  ┌──────────┐          ┌──────────┐          ┌──────────┐         │
│  │ Grafana  │          │  Kibana  │          │ Jaeger UI│         │
│  │ 监控面板  │          │ 日志分析  │          │ 链路追踪  │         │
│  └──────────┘          └──────────┘          └──────────┘         │
└─────────────────────────────────────────────────────────────────┘
```

## 1. 指标监控 (Prometheus + Grafana)

### Spring Boot Actuator + Micrometer

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    tags:
      application: ${spring.application.name}
    distribution:
      slo:
        http: 50ms,100ms,200ms,500ms,1s,5s
```

### 自定义业务指标

```java
@Component
public class FileProcessingMetrics {
    
    private final Counter uploadCounter;
    private final Counter conversionCounter;
    private final Timer processingTimer;
    private final Gauge storageGauge;
    
    public FileProcessingMetrics(MeterRegistry registry) {
        this.uploadCounter = Counter.builder("file.upload.total")
            .description("Total file uploads")
            .register(registry);
        
        this.conversionCounter = Counter.builder("file.conversion.total")
            .description("Total file conversions")
            .tag("status", "success")
            .register(registry);
        
        this.processingTimer = Timer.builder("file.processing.duration")
            .description("File processing time")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
        
        this.storageGauge = Gauge.builder("storage.used.bytes")
            .description("Storage space used")
            .register(registry, this, FileProcessingMetrics::getStorageUsed);
    }
    
    public void recordUpload(String fileType, long fileSize) {
        uploadCounter.increment();
        registry.counter("file.upload.by_type", "type", fileType).increment();
        registry.distributionSummary("file.upload.size", "type", fileType)
            .record(fileSize);
    }
    
    public void recordConversion(String fromFormat, String toFormat, boolean success) {
        conversionCounter.increment();
        registry.counter("file.conversion.by_format", 
            "from", fromFormat, 
            "to", toFormat,
            "status", success ? "success" : "failed"
        ).increment();
    }
    
    public Timer.Sample startProcessing() {
        return Timer.start(registry);
    }
    
    public void stopProcessing(Timer.Sample sample, String operation) {
        sample.stop(registry.timer("file.processing.duration", "operation", operation));
    }
    
    private double getStorageUsed() {
        // 返回已用存储空间
        return storageService.getUsedSpace();
    }
}
```

### Prometheus 配置

```yaml
# prometheus/prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'file-master'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['file-master:8080']
    scrape_interval: 5s

  - job_name: 'file-worker'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['file-worker:8081']

  - job_name: 'mysql'
    static_configs:
      - targets: ['mysql-exporter:9104']

  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']

  - job_name: 'node'
    static_configs:
      - targets: ['node-exporter:9100']
```

### Grafana 仪表盘

```json
{
  "dashboard": {
    "title": "FileMaster Pro - 系统监控",
    "panels": [
      {
        "title": "API 请求速率",
        "targets": [
          {
            "expr": "rate(http_server_requests_seconds_count[5m])",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "title": "API 响应时间 (P99)",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))",
            "legendFormat": "{{method}} {{uri}}"
          }
        ]
      },
      {
        "title": "文件上传统计",
        "targets": [
          {
            "expr": "file_upload_total",
            "legendFormat": "总上传数"
          },
          {
            "expr": "rate(file_upload_total[5m])",
            "legendFormat": "上传速率"
          }
        ]
      },
      {
        "title": "存储空间使用",
        "targets": [
          {
            "expr": "storage_used_bytes",
            "legendFormat": "已用空间"
          }
        ]
      },
      {
        "title": "JVM 内存",
        "targets": [
          {
            "expr": "jvm_memory_used_bytes",
            "legendFormat": "{{area}} {{id}}"
          }
        ]
      },
      {
        "title": "任务队列深度",
        "targets": [
          {
            "expr": "rabbitmq_queue_messages",
            "legendFormat": "{{queue}}"
          }
        ]
      }
    ]
  }
}
```

## 2. 日志收集 (ELK Stack)

### Logback 配置

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeContext>true</includeContext>
            <includeMdc>true</includeMdc>
            <customFields>{"service":"${SERVICE_NAME:-file-master}","version":"${VERSION:-1.0.0}"}</customFields>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/filemaster/application.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/filemaster/application.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxHistory>30</maxHistory>
            <maxFileSize>100MB</maxFileSize>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>
    
    <springProfile name="docker">
        <root level="INFO">
            <appender-ref ref="JSON"/>
        </root>
    </springProfile>
    
    <springProfile name="local">
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
            <appender-ref ref="FILE"/>
        </root>
    </springProfile>
</configuration>
```

### Logstash 配置

```ruby
# logstash/logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  if [fields][log_type] == "filemaster" {
    json {
      source => "message"
      skip_on_invalid_json => true
    }
    
    date {
      match => ["@timestamp", "ISO8601"]
    }
    
    mutate {
      add_field => {
        "service" => "%{[fields][service]}"
        "environment" => "%{[fields][environment]}"
      }
    }
  }
}

output {
  elasticsearch {
    hosts => ["http://elasticsearch:9200"]
    index => "filemaster-%{+YYYY.MM.dd}"
  }
}
```

### Filebeat 配置

```yaml
# filebeat/filebeat.yml
filebeat.inputs:
  - type: log
    enabled: true
    paths:
      - /var/log/filemaster/*.log
    fields:
      log_type: filemaster
      service: file-master
      environment: production
    fields_under_root: true
    json.keys_under_root: true

output.logstash:
  hosts: ["logstash:5044"]
```

## 3. 分布式追踪 (Jaeger)

```java
@Configuration
public class TracingConfig {
    
    @Bean
    public JaegerTracer jaegerTracer() {
        io.jaegertracing.Configuration.SamplerConfiguration samplerConfig = 
            io.jaegertracing.Configuration.SamplerConfiguration.fromEnv()
                .withType("const")
                .withParam(1);
        
        io.jaegertracing.Configuration.ReporterConfiguration reporterConfig = 
            io.jaegertracing.Configuration.ReporterConfiguration.fromEnv()
                .withLogSpans(true)
                .withSender(
                    io.jaegertracing.Configuration.SenderConfiguration.fromEnv()
                        .withAgentHost("jaeger-agent")
                        .withAgentPort(6831)
                );
        
        io.jaegertracing.Configuration config = new io.jaegertracing.Configuration("file-master")
            .withSampler(samplerConfig)
            .withReporter(reporterConfig);
        
        return config.getTracer();
    }
}

// 使用注解追踪
@Service
public class FileConvertService {
    
    @SpanTag("convert.format")
    public FileResponse convert(@SpanTag("source.path") String sourcePath,
                               @SpanTag("target.format") String targetFormat) {
        // 转换逻辑
    }
}
```

## 4. 健康检查

```java
@Component
public class StorageHealthIndicator implements HealthIndicator {
    
    @Autowired
    private StorageService storageService;
    
    @Override
    public Health health() {
        try {
            long freeSpace = storageService.getFreeSpace();
            long totalSpace = storageService.getTotalSpace();
            double usagePercent = (totalSpace - freeSpace) * 100.0 / totalSpace;
            
            Health.Builder builder = usagePercent > 90 ? Health.down() : Health.up();
            
            return builder
                .withDetail("freeSpace", freeSpace)
                .withDetail("totalSpace", totalSpace)
                .withDetail("usagePercent", String.format("%.2f%%", usagePercent))
                .build();
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}

@Component
public class FFmpegHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        try {
            Process process = Runtime.getRuntime().exec("ffmpeg -version");
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                return Health.up()
                    .withDetail("ffmpeg", "available")
                    .build();
            } else {
                return Health.down()
                    .withDetail("ffmpeg", "not available")
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }
}
```

## 5. 告警规则 (Prometheus AlertManager)

```yaml
# prometheus/alert-rules.yml
groups:
  - name: filemaster
    rules:
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected"
          description: "Error rate is {{ $value }} errors per second"

      - alert: HighResponseTime
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m])) > 5
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High response time"
          description: "P99 response time is {{ $value }}s"

      - alert: DiskSpaceLow
        expr: (storage_used_bytes / storage_total_bytes) > 0.85
        for: 1m
        labels:
          severity: warning
        annotations:
          summary: "Disk space is running low"
          description: "Disk usage is above 85%"

      - alert: MemoryUsageHigh
        expr: (jvm_memory_used_bytes / jvm_memory_max_bytes) > 0.9
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High memory usage"
          description: "JVM memory usage is above 90%"

      - alert: TaskQueueBacklog
        expr: rabbitmq_queue_messages > 1000
        for: 10m
        labels:
          severity: warning
        annotations:
          summary: "Task queue backlog"
          description: "Task queue has {{ $value }} pending messages"

      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service is down"
          description: "{{ $labels.job }} is down"
```

### AlertManager 配置

```yaml
# prometheus/alertmanager.yml
global:
  smtp_smarthost: 'smtp.gmail.com:587'
  smtp_from: 'alerts@filemaster.pro'
  smtp_auth_username: 'alerts@filemaster.pro'
  smtp_auth_password: '${SMTP_PASSWORD}'

route:
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default'
  routes:
    - match:
        severity: critical
      receiver: 'pagerduty'
      continue: true

receivers:
  - name: 'default'
    email_configs:
      - to: 'ops@filemaster.pro'
        headers:
          Subject: '[FileMaster Alert] {{ .GroupLabels.alertname }}'

  - name: 'pagerduty'
    pagerduty_configs:
      - service_key: '${PAGERDUTY_KEY}'
        severity: critical

  - name: 'slack'
    slack_configs:
      - api_url: '${SLACK_WEBHOOK_URL}'
        channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'
```

## 6. 监控面板

### 关键指标

| 类别 | 指标 | 告警阈值 |
|------|------|----------|
| **API** | 请求速率 | - |
| | P99 响应时间 | > 5s |
| | 错误率 | > 10% |
| **文件处理** | 上传成功率 | < 95% |
| | 转换成功率 | < 90% |
| | 平均处理时间 | > 60s |
| **存储** | 磁盘使用率 | > 85% |
| | 存储增长速率 | - |
| **队列** | 任务队列深度 | > 1000 |
| | 消费速率 | < 生产速率 |
| **JVM** | 堆内存使用 | > 90% |
| | GC 暂停时间 | > 1s |
| **系统** | CPU 使用率 | > 80% |
| | 内存使用率 | > 85% |

## 验收标准

- [ ] Prometheus 指标采集
- [ ] Grafana 监控仪表盘
- [ ] ELK 日志收集与分析
- [ ] Jaeger 分布式追踪
- [ ] 健康检查端点
- [ ] 告警规则配置
- [ ] 告警通知（邮件/Slack/PagerDuty）
- [ ] 日志聚合查询
- [ ] 链路追踪可视化
