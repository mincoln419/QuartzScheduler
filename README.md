# Spring Scheduler - 웹 레이어 없는 배치 스케줄러

## 개요

이 프로젝트는 **웹 레이어 없는** Spring Boot + Scheduling 아키텍처를 구현한 배치 스케줄러입니다. 외부 API 동시 호출이 많은 환경에서 WebClient(Reactor)를 활용하여 효율적인 비동기 처리를 제공합니다.

## 아키텍처 특징

### 🏗️ 핵심 설계 원칙

- **웹 서버 없음**: MVC/WebFlux 서버 없이 순수 배치 처리에 집중
- **파일 기반 설정**: YAML 설정 파일로 동적 잡 관리
- **단일 인스턴스**: DB 없이 파일 기반 상태 관리
- **높은 동시성**: WebClient를 통한 논블로킹 I/O 처리

### 📋 아키텍처 레이어

```
┌─────────────────────────────────────────┐
│           스케줄링 레이어                │
│  @EnableScheduling + Quartz (옵션)      │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│           잡 실행 레이어                │
│  ThreadPoolTaskExecutor + WebClient     │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│         설정/상태 레이어                │
│  YAML 설정 + 파일 기반 체크포인트        │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│         관찰/운영 레이어                │
│  Actuator (헬스체크, 메트릭)            │
└─────────────────────────────────────────┘
```

## 프로젝트 구조

### 📁 개발 디렉토리

```
spring_scheduler/
├── src/main/java/com/ideatec/spring_scheduler/
│   ├── config/                              # 설정 관리
│   │   ├── JobConfig.java                   # YAML 설정 POJO
│   │   └── JobConfigLoader.java             # 설정 파일 로더
│   ├── scheduler/                           # 스케줄러
│   │   ├── SchedulerConfig.java             # Quartz 설정
│   │   ├── QuartzDynamicScheduler.java      # 동적 스케줄러
│   │   └── QuartzJobWrapper.java            # Quartz Job 래퍼
│   ├── client/                              # 외부 API 클라이언트
│   │   └── ReactiveWebClientClient.java     # WebClient 기반 HTTP 클라이언트
│   ├── checkpoint/                          # 상태 관리
│   │   └── CheckpointStore.java             # 파일 기반 체크포인트
│   └── actuator/                            # 모니터링
│       └── JobMetrics.java                  # 잡 실행 메트릭
├── src/main/resources/
│   ├── application.yml                      # 애플리케이션 설정
│   └── scheduler-jobs.yml                   # 잡 설정 (템플릿)
├── runnable/                                # 실행 가능한 아티팩트
│   ├── spring_scheduler-0.0.1-SNAPSHOT.jar # JAR 파일
│   ├── scheduler-jobs.yml                   # 외부 설정 파일 (수정 가능)
│   ├── application.yml                      # 애플리케이션 설정
│   ├── start.sh                            # 서버 시작
│   ├── stop.sh                             # 서버 종료
│   ├── status.sh                           # 서버 상태 확인
│   ├── reload.sh                           # 설정 리로드
│   ├── deploy.sh                           # 배포 스크립트
│   ├── logs/                               # 로그 디렉토리
│   └── README.md                           # 사용법 가이드
└── *.sh                                    # 개발용 스크립트
```

### 🚀 실행 가능한 아티팩트

`runnable` 디렉토리는 **완전히 독립적인 실행 환경**을 제공합니다:

- ✅ **JAR 파일**: Spring Boot 애플리케이션
- ✅ **설정 파일**: 외부에서 수정 가능한 YAML 설정
- ✅ **스크립트**: 서버 관리용 쉘 스크립트
- ✅ **로그**: 자동 생성되는 로그 디렉토리
- ✅ **문서**: 사용법 가이드 포함

## 주요 컴포넌트

### 1. 설정 관리 (`config/`)

- **JobConfig**: YAML 설정 파일을 POJO로 매핑
- **JobConfigLoader**: 외부 설정 파일 우선 로드, 변경 감지 및 캐싱

### 2. 스케줄러 (`scheduler/`)

- **SchedulerConfig**: Quartz 스케줄러 설정
- **QuartzDynamicScheduler**: YAML 설정 기반 동적 스케줄링
- **QuartzJobWrapper**: Quartz Job 실행 래퍼

### 3. 외부 API 클라이언트 (`client/`)

- **ReactiveWebClientClient**: WebClient 기반 비동기 HTTP 클라이언트

### 4. 상태 관리 (`checkpoint/`)

- **CheckpointStore**: 파일 기반 원자적 상태 저장

### 5. 모니터링 (`actuator/`)

- **JobMetrics**: Micrometer 기반 잡 실행 메트릭

## 설정 파일

### `scheduler-jobs.yml`

```yaml
timezone: Asia/Seoul
jobs:
  - id: fetch-users
    cron: "0 */5 * * * *" # 5분마다
    parallelism: 8
    request:
      url: "https://api.example.com/users"
      method: GET
      timeoutMs: 3000
      retry:
        max: 3
        backoffMs: 200
  - id: push-report
    cron: "0 0 2 * * *" # 매일 02:00
    parallelism: 2
    request:
      url: "https://api.example.com/report"
      method: POST
      bodyFile: "data/report.json"
      timeoutMs: 5000
      retry:
        max: 2
        backoffMs: 500
```

## 실행 방법

### 1. 배포 (최초 1회)

```bash
./deploy.sh
```

- 프로젝트 빌드
- JAR 파일과 설정 파일을 `runnable` 디렉토리로 복사
- 스크립트 실행 권한 부여

### 2. 실행 가능한 아티팩트 사용

```bash
cd runnable
./start.sh
```

### 3. 서버 종료

```bash
./stop.sh
```

### 4. 서버 상태 확인

```bash
./status.sh
```

### 5. 설정 리로드

```bash
./reload.sh
```

### 6. 헬스체크

```bash
curl http://localhost:28732/actuator/health
```

### 7. 메트릭 확인

```bash
curl http://localhost:28732/actuator/metrics
```

## 주요 기능

### ✅ 웹 레이어 없는 아키텍처

- MVC/WebFlux 서버 없이 순수 배치 처리에 집중
- 웹 서버의 복잡성 제거로 운영 부담 최소화

### ✅ Quartz 기반 동적 스케줄링

- YAML 설정 파일의 cron 표현식 실제 사용
- 설정 파일 변경 시 자동 리스케줄링
- 매분마다 설정 파일 스캔 및 변경 감지

### ✅ 외부 설정 파일 지원

- JAR 외부에서 설정 파일 수정 가능
- 실시간 설정 변경 및 리로드
- 외부 파일 우선, JAR 내부 리소스 백업

### ✅ 병렬 처리

- 잡별 독립적인 병렬도 설정
- Quartz Job 기반 스레드 관리
- 높은 동시성 처리 가능

### ✅ 비동기 I/O

- WebClient를 통한 논블로킹 HTTP 호출
- Reactor Netty 기반 고성능 네트워킹
- 외부 API 동시 호출 최적화

### ✅ 상태 관리

- 파일 기반 원자적 체크포인트 저장
- 재시작 후 상태 복구 지원
- 단일 인스턴스 전제로 중복 실행 방지

### ✅ 모니터링

- Actuator 기반 헬스체크 및 메트릭
- 잡 실행 성공/실패/시간 메트릭
- 포트 28732에서 서비스 제공

### ✅ 실행 가능한 아티팩트

- 완전히 독립적인 실행 환경
- JAR 파일과 스크립트가 함께 패키징
- 어디서든 실행 가능한 배포 단위

## 의존성

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-quartz'
    implementation 'org.springframework:spring-webflux'
    implementation 'io.netty:netty-all'
    implementation 'io.projectreactor.netty:reactor-netty-http'
    implementation 'com.fasterxml.jackson.dataformat:jackson-dataformat-yaml'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

## 운영 가이드

### 🔧 설정 변경

1. `src/main/resources/scheduler-jobs.yml` 수정
2. 애플리케이션 재시작 또는 설정 리로드

### 📊 모니터링

- **헬스체크**: `GET /actuator/health`
- **메트릭**: `GET /actuator/metrics`
- **상태 파일**: `state/checkpoints.json`

### 🚨 장애 대응

- **단일 인스턴스**: 중복 실행 방지를 위해 단일 인스턴스 운영
- **재시작 내구성**: 파일 기반 체크포인트로 상태 복구
- **외부 시스템**: 멱등키/업서트 패턴으로 중복 처리 방지

## 장점

### 🎯 단순성

- 웹 레이어 제거로 복잡도 최소화
- 파일 기반 설정으로 운영 단순화

### ⚡ 성능

- WebClient 기반 논블로킹 I/O
- 높은 동시성 처리 가능

### 🛡️ 안정성

- 단일 인스턴스로 중복 실행 방지
- 파일 기반 상태 관리로 장애 복구

### 📈 확장성

- Quartz 통합으로 정교한 스케줄링
- 설정 기반 동적 잡 관리

## 제한사항

- **단일 인스턴스**: 클러스터링 미지원
- **메모리 상태**: 재시작 시 스케줄 정보 초기화 (Quartz RAMJobStore)
- **파일 의존성**: 상태 파일 손상 시 복구 필요

## 결론

이 아키텍처는 **웹 레이어가 불필요한 배치 작업**에 최적화되어 있습니다.

### 🎯 핵심 가치

- **단순성**: 웹 서버 없이 순수 배치 처리에 집중
- **유연성**: 외부 설정 파일로 실시간 잡 관리
- **성능**: WebClient 기반 논블로킹 I/O로 높은 동시성
- **안정성**: Quartz 기반 정교한 스케줄링과 파일 기반 상태 관리
- **운영성**: 완전히 독립적인 실행 가능한 아티팩트

### 🚀 적용 시나리오

- **외부 API 동시 호출**이 많은 배치 작업
- **웹 서버가 불필요**한 순수 스케줄링 시스템
- **설정 변경이 빈번**한 동적 잡 관리
- **단일 인스턴스**로 충분한 배치 처리

외부 API 동시 호출이 많은 환경에서 WebClient의 논블로킹 특성을 활용하여 높은 처리량을 달성하면서도, 웹 서버의 복잡성을 제거하여 운영 부담을 최소화했습니다.
