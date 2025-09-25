#!/bin/bash

# Spring Scheduler 서버 종료 스크립트

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 스크립트가 있는 디렉토리로 이동
cd "$(dirname "$0")"

echo -e "${YELLOW}🛑 Spring Scheduler 서버를 종료합니다...${NC}"

# PID 파일 확인
if [ -f "server.pid" ]; then
    PID=$(cat server.pid)
    if ps -p $PID > /dev/null 2>&1; then
        echo -e "${YELLOW}📡 서버 종료 중... (PID: $PID)${NC}"
        kill -TERM $PID
        
        # 종료 대기 (최대 30초)
        for i in {1..30}; do
            if ! ps -p $PID > /dev/null 2>&1; then
                echo -e "${GREEN}✅ 서버가 정상적으로 종료되었습니다!${NC}"
                rm -f server.pid
                exit 0
            fi
            sleep 1
            echo -n "."
        done
        
        # 강제 종료
        echo -e "\n${RED}⚠️  정상 종료에 실패했습니다. 강제 종료합니다.${NC}"
        kill -KILL $PID
        sleep 2
        
        if ! ps -p $PID > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 서버가 강제 종료되었습니다!${NC}"
            rm -f server.pid
        else
            echo -e "${RED}❌ 서버 종료에 실패했습니다.${NC}"
            exit 1
        fi
    else
        echo -e "${YELLOW}⚠️  PID 파일은 있지만 해당 프로세스가 실행되지 않습니다.${NC}"
        rm -f server.pid
    fi
else
    # PID 파일이 없는 경우 프로세스 이름으로 찾기
    PID=$(ps aux | grep "spring_scheduler-0.0.1-SNAPSHOT.jar" | grep -v grep | awk '{print $2}')
    if [ ! -z "$PID" ]; then
        echo -e "${YELLOW}📡 서버 종료 중... (PID: $PID)${NC}"
        kill -TERM $PID
        sleep 5
        
        if ! ps -p $PID > /dev/null 2>&1; then
            echo -e "${GREEN}✅ 서버가 정상적으로 종료되었습니다!${NC}"
        else
            echo -e "${RED}⚠️  정상 종료에 실패했습니다. 강제 종료합니다.${NC}"
            kill -KILL $PID
            sleep 2
            echo -e "${GREEN}✅ 서버가 강제 종료되었습니다!${NC}"
        fi
    else
        echo -e "${YELLOW}⚠️  실행 중인 서버를 찾을 수 없습니다.${NC}"
    fi
fi
