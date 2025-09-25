#!/bin/bash

# Spring Scheduler 설정 리로드 스크립트

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 스크립트가 있는 디렉토리로 이동
cd "$(dirname "$0")"

echo -e "${BLUE}🔄 Spring Scheduler 설정을 리로드합니다...${NC}"

# 설정 파일 존재 확인
if [ ! -f "scheduler-jobs.yml" ]; then
    echo -e "${RED}❌ scheduler-jobs.yml 파일을 찾을 수 없습니다.${NC}"
    echo -e "${YELLOW}💡 먼저 ./deploy.sh를 실행하세요.${NC}"
    exit 1
fi

# 서버 실행 중인지 확인
PID=$(ps aux | grep "spring_scheduler-0.0.1-SNAPSHOT.jar" | grep -v grep | awk '{print $2}')
if [ -z "$PID" ]; then
    echo -e "${YELLOW}⚠️  서버가 실행되지 않았습니다.${NC}"
    echo -e "${YELLOW}💡 먼저 ./start.sh로 서버를 시작하세요.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 서버 실행 중 (PID: $PID)${NC}"

# 설정 파일 문법 검사 (간단한 YAML 검사)
echo -e "${BLUE}🔍 설정 파일 문법을 검사합니다...${NC}"
if command -v yq >/dev/null 2>&1; then
    if yq eval '.' scheduler-jobs.yml >/dev/null 2>&1; then
        echo -e "${GREEN}✅ YAML 문법이 올바릅니다.${NC}"
    else
        echo -e "${RED}❌ YAML 문법 오류가 있습니다.${NC}"
        echo -e "${YELLOW}💡 설정 파일을 확인하세요.${NC}"
        exit 1
    fi
else
    echo -e "${YELLOW}⚠️  yq가 설치되지 않아 문법 검사를 건너뜁니다.${NC}"
fi

# 설정 파일 변경 시간을 현재 시간으로 업데이트 (강제 리로드 트리거)
touch scheduler-jobs.yml

echo -e "${GREEN}✅ 설정 파일이 업데이트되었습니다.${NC}"
echo -e "${BLUE}📋 서버가 1분 이내에 새로운 설정을 로드합니다.${NC}"
echo -e "${YELLOW}💡 서버 로그를 확인하세요: tail -f logs/server.log${NC}"
