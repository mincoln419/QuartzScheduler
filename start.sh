#!/bin/bash

# Spring Scheduler 서버 시작 스크립트

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 스크립트가 있는 디렉토리로 이동
cd "$(dirname "$0")"

echo -e "${GREEN}🚀 Spring Scheduler 서버를 시작합니다...${NC}"

# JAR 파일 존재 확인
if [ ! -f "spring_scheduler-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${RED}❌ JAR 파일을 찾을 수 없습니다.${NC}"
    echo -e "${YELLOW}💡 JAR 파일이 스크립트와 같은 디렉토리에 있는지 확인하세요.${NC}"
    exit 1
fi

# 기존 프로세스 확인 및 종료
PID=$(ps aux | grep "spring_scheduler-0.0.1-SNAPSHOT.jar" | grep -v grep | awk '{print $2}')
if [ ! -z "$PID" ]; then
    echo -e "${YELLOW}⚠️  기존 서버가 실행 중입니다. 종료 후 재시작합니다.${NC}"
    kill -TERM $PID
    sleep 3
fi

# 서버 시작
echo -e "${GREEN}📡 서버를 시작합니다...${NC}"
nohup java -jar spring_scheduler-0.0.1-SNAPSHOT.jar > logs/server.log 2>&1 &

# PID 저장
echo $! > server.pid

# 서버 시작 확인
sleep 5
if ps -p $(cat server.pid) > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 서버가 성공적으로 시작되었습니다!${NC}"
    echo -e "${GREEN}📊 헬스체크: curl http://localhost:8080/actuator/health${NC}"
    echo -e "${GREEN}📈 메트릭: curl http://localhost:8080/actuator/metrics${NC}"
    echo -e "${GREEN}📝 로그: tail -f logs/server.log${NC}"
    echo -e "${GREEN}🛑 종료: ./stop.sh${NC}"
else
    echo -e "${RED}❌ 서버 시작에 실패했습니다. 로그를 확인하세요.${NC}"
    echo -e "${YELLOW}📝 로그: cat logs/server.log${NC}"
    exit 1
fi
