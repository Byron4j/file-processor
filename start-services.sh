#!/bin/bash

# FileMaster Pro 服务启动脚本
# 同时启动前后端服务

echo "🚀 FileMaster Pro 服务启动脚本"
echo "================================"

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查端口占用
if lsof -i :8080 -sTCP:LISTEN > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  端口 8080 已被占用，后端可能已在运行${NC}"
else
    echo -e "${BLUE}▶️  正在启动后端服务...${NC}"
    cd "$(dirname "$0")/file-processor"
    mvn spring-boot:run > /tmp/file-processor.log 2>&1 &
    echo $! > /tmp/file-processor.pid
    sleep 5
    if curl -s http://localhost:8080/api/file/health > /dev/null; then
        echo -e "${GREEN}✅ 后端服务启动成功 (PID: $(cat /tmp/file-processor.pid))${NC}"
        echo -e "${GREEN}   访问地址: http://localhost:8080${NC}"
    else
        echo -e "${YELLOW}⚠️  后端服务启动中，请稍后检查...${NC}"
    fi
fi

echo ""

if lsof -i :3000 -sTCP:LISTEN > /dev/null 2>&1; then
    echo -e "${YELLOW}⚠️  端口 3000 已被占用，前端可能已在运行${NC}"
else
    echo -e "${BLUE}▶️  正在启动前端服务...${NC}"
    cd "$(dirname "$0")/filemaster-web"
    npm run dev > /tmp/filemaster-web.log 2>&1 &
    echo $! > /tmp/filemaster-web.pid
    sleep 3
    if curl -s http://localhost:3000 > /dev/null; then
        echo -e "${GREEN}✅ 前端服务启动成功 (PID: $(cat /tmp/filemaster-web.pid))${NC}"
        echo -e "${GREEN}   访问地址: http://localhost:3000${NC}"
    else
        echo -e "${YELLOW}⚠️  前端服务启动中，请稍后检查...${NC}"
    fi
fi

echo ""
echo "================================"
echo "📋 服务状态:"
echo "   后端 API: http://localhost:8080"
echo "   前端页面: http://localhost:3000"
echo "   数据库控制台: http://localhost:8080/h2-console"
echo ""
echo "📖 查看演示手册: ./演示操作手册.md"
echo ""
echo "🛑 停止服务:"
echo "   后端: kill $(cat /tmp/file-processor.pid 2>/dev/null || echo 'PID未找到')"
echo "   前端: kill $(cat /tmp/filemaster-web.pid 2>/dev/null || echo 'PID未找到')"
echo "================================"
