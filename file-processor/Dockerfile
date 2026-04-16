# Build stage
FROM maven:3.9-eclipse-temurin-21-alpine AS builder

WORKDIR /app

# Copy pom.xml first for better layer caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Add security updates and ffmpeg for media processing
RUN apk update && apk upgrade && \
    apk add --no-cache \
        ffmpeg \
        tesseract-ocr \
        tesseract-ocr-data-eng \
        tesseract-ocr-data-chi_sim \
        tzdata && \
    rm -rf /var/cache/apk/*

# Create non-root user
RUN addgroup -g 1001 -S filemaster && \
    adduser -u 1001 -S filemaster -G filemaster

WORKDIR /app

# Copy JAR from builder
COPY --from=builder /app/target/file-processor-*.jar app.jar

# Create uploads directory with proper permissions
RUN mkdir -p /app/uploads && \
    chown -R filemaster:filemaster /app

# Switch to non-root user
USER filemaster

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/file/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+OptimizeStringConcat -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
