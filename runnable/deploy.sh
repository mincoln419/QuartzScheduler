#!/bin/bash

# Spring Scheduler 배포 스크립트 (빌드 + JAR 복사)

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 스크립트가 있는 디렉토리로 이동
cd "$(dirname "$0")"

echo -e "${BLUE}🔨 Spring Scheduler 배포를 시작합니다...${NC}"

# 1. 빌드 실행
echo -e "${YELLOW}📦 프로젝트를 빌드합니다...${NC}"
if ./gradlew clean build --no-daemon; then
    echo -e "${GREEN}✅ 빌드 성공!${NC}"
else
    echo -e "${RED}❌ 빌드 실패!${NC}"
    exit 1
fi

# 2. JAR 파일을 현재 디렉토리로 복사
echo -e "${YELLOW}📋 JAR 파일을 복사합니다...${NC}"
if cp build/libs/spring_scheduler-0.0.1-SNAPSHOT.jar .; then
    echo -e "${GREEN}✅ JAR 파일 복사 완료!${NC}"
else
    echo -e "${RED}❌ JAR 파일 복사 실패!${NC}"
    exit 1
fi

# 3. 설정 파일을 현재 디렉토리로 복사 (외부 설정 파일로 사용)
echo -e "${YELLOW}📋 설정 파일을 복사합니다...${NC}"
if cp src/main/resources/scheduler-jobs.yml .; then
    echo -e "${GREEN}✅ 설정 파일 복사 완료!${NC}"
    echo -e "${BLUE}💡 이제 scheduler-jobs.yml을 직접 수정할 수 있습니다!${NC}"
else
    echo -e "${RED}❌ 설정 파일 복사 실패!${NC}"
    exit 1
fi

# 4. logs 디렉토리 생성
if [ ! -d "logs" ]; then
    mkdir -p logs
    echo -e "${GREEN}📁 logs 디렉토리를 생성했습니다.${NC}"
fi

# 5. 스크립트 실행 권한 부여
chmod +x start.sh stop.sh status.sh
echo -e "${GREEN}🔐 스크립트 실행 권한을 부여했습니다.${NC}"

echo -e "${GREEN}🎉 배포 완료!${NC}"
echo -e "${BLUE}📋 사용 가능한 명령어:${NC}"
echo -e "  ${GREEN}./start.sh${NC}  - 서버 시작"
echo -e "  ${GREEN}./stop.sh${NC}   - 서버 종료"
echo -e "  ${GREEN}./status.sh${NC} - 서버 상태 확인"
echo ""
echo -e "${YELLOW}💡 이제 ./start.sh로 서버를 시작할 수 있습니다!${NC}"
