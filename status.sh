#!/bin/bash

# Spring Scheduler 서버 상태 확인 스크립트

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 스크립트가 있는 디렉토리로 이동
cd "$(dirname "$0")"

echo -e "${BLUE}📊 Spring Scheduler 서버 상태${NC}"
echo "=================================="

# PID 파일 확인
if [ -f "server.pid" ]; then
    PID=$(cat server.pid)
    if ps -p $PID > /dev/null 2>&1; then
        echo -e "${GREEN}✅ 서버 실행 중 (PID: $PID)${NC}"
        
        # 헬스체크
        echo -e "${BLUE}🏥 헬스체크 확인 중...${NC}"
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 헬스체크 통과${NC}"
            
            # 헬스체크 상세 정보
            echo -e "${BLUE}📋 헬스체크 상세:${NC}"
            curl -s http://localhost:8080/actuator/health | jq . 2>/dev/null || curl -s http://localhost:8080/actuator/health
        else
            echo -e "${RED}❌ 헬스체크 실패${NC}"
        fi
        
        # 메트릭 확인
        echo -e "${BLUE}📈 잡 메트릭 확인 중...${NC}"
        METRICS=$(curl -s http://localhost:8080/actuator/metrics 2>/dev/null | grep -o '"job\.[^"]*"' | head -5)
        if [ ! -z "$METRICS" ]; then
            echo -e "${GREEN}✅ 잡 메트릭 발견:${NC}"
            echo "$METRICS" | sed 's/^/  - /'
        else
            echo -e "${YELLOW}⚠️  아직 잡 메트릭이 없습니다 (정상 - 잡이 아직 실행되지 않음)${NC}"
        fi
        
        # 로그 파일 확인
        if [ -f "logs/server.log" ]; then
            echo -e "${BLUE}📝 최근 로그 (마지막 5줄):${NC}"
            tail -5 logs/server.log | sed 's/^/  /'
        fi
        
    else
        echo -e "${RED}❌ PID 파일은 있지만 프로세스가 실행되지 않습니다${NC}"
        rm -f server.pid
    fi
else
    # PID 파일이 없는 경우 프로세스 이름으로 찾기
    PID=$(ps aux | grep "spring_scheduler-0.0.1-SNAPSHOT.jar" | grep -v grep | awk '{print $2}')
    if [ ! -z "$PID" ]; then
        echo -e "${YELLOW}⚠️  서버 실행 중이지만 PID 파일이 없습니다 (PID: $PID)${NC}"
        echo -e "${YELLOW}💡 정상적인 종료를 위해 ./stop.sh를 실행하세요${NC}"
    else
        echo -e "${RED}❌ 서버가 실행되지 않았습니다${NC}"
        echo -e "${YELLOW}💡 서버 시작: ./start.sh${NC}"
    fi
fi

echo "=================================="
