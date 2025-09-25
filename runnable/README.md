# Spring Scheduler - 실행 가능한 아티팩트

## 📦 포함된 파일

```
runnable/
├── spring_scheduler-0.0.1-SNAPSHOT.jar    # Spring Boot 애플리케이션
├── scheduler-jobs.yml                      # 잡 설정 파일 (수정 가능)
├── start.sh                               # 서버 시작
├── stop.sh                                # 서버 종료
├── status.sh                              # 서버 상태 확인
├── reload.sh                              # 설정 리로드
├── deploy.sh                              # 배포 스크립트 (개발용)
├── logs/                                  # 로그 디렉토리
└── README.md                              # 이 파일
```

## 🚀 빠른 시작

### 1. 서버 시작
```bash
./start.sh
```

### 2. 서버 상태 확인
```bash
./status.sh
```

### 3. 서버 종료
```bash
./stop.sh
```

## ⚙️ 설정 수정

### 잡 설정 변경
1. `scheduler-jobs.yml` 파일을 편집
2. 설정 리로드: `./reload.sh`

### 설정 파일 예시
```yaml
timezone: Asia/Seoul
jobs:
  - id: fetch-users
    cron: "0 * * * * *"   # 1분마다
    parallelism: 8
    request:
      url: "https://api.example.com/users"
      method: GET
      timeoutMs: 3000
      retry:
        max: 3
        backoffMs: 200
```

## 📊 모니터링

### 헬스체크
```bash
curl http://localhost:8080/actuator/health
```

### 메트릭 확인
```bash
curl http://localhost:8080/actuator/metrics
```

### 로그 확인
```bash
tail -f logs/server.log
```

## 🔧 고급 사용법

### 포트 변경
애플리케이션 실행 시 포트를 변경하려면:
```bash
java -jar spring_scheduler-0.0.1-SNAPSHOT.jar --server.port=8081
```

### JVM 옵션 설정
```bash
java -Xmx512m -Xms256m -jar spring_scheduler-0.0.1-SNAPSHOT.jar
```

### 프로파일 설정
```bash
java -jar spring_scheduler-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## 📋 주요 기능

- ✅ **웹 레이어 없는** Spring Boot + Quartz 아키텍처
- ✅ **외부 설정 파일** 지원 (JAR 외부에서 수정 가능)
- ✅ **동적 스케줄링** (설정 변경 시 자동 리스케줄링)
- ✅ **병렬 처리** (잡별 독립적인 병렬도 설정)
- ✅ **비동기 I/O** (WebClient 기반 논블로킹 HTTP 호출)
- ✅ **상태 관리** (파일 기반 체크포인트)
- ✅ **모니터링** (Actuator 기반 헬스체크 및 메트릭)

## 🛠️ 문제 해결

### 서버가 시작되지 않는 경우
1. 포트 충돌 확인: `lsof -i :8080`
2. 로그 확인: `cat logs/server.log`
3. Java 버전 확인: `java -version` (Java 17 필요)

### 설정이 적용되지 않는 경우
1. YAML 문법 확인
2. `./reload.sh` 실행
3. 서버 재시작: `./stop.sh && ./start.sh`

### 메모리 부족 시
```bash
java -Xmx1g -Xms512m -jar spring_scheduler-0.0.1-SNAPSHOT.jar
```

## 📞 지원

- **아키텍처**: 웹 레이어 없는 Spring Boot + Quartz
- **설정**: YAML 기반 외부 설정 파일
- **모니터링**: Actuator 엔드포인트
- **로그**: `logs/server.log`
