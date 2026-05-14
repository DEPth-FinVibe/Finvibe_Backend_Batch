# Deployment Guide

이 문서는 `finvibe-backend-batch` 배포, 실행, 운영 설정을 정리한다.

## Runtime

| 항목 | 값 |
| --- | --- |
| Java | 21 |
| Framework | Spring Boot 4.0.4 |
| Build | Gradle Wrapper |
| Main class | `depth.finvibe.FinvibeApplication` |
| Container base | `eclipse-temurin:21-jre` |

## Build

로컬 jar 빌드:

```bash
./gradlew clean bootJar
```

테스트:

```bash
./gradlew test
```

Docker image 빌드:

```bash
docker build -t finvibe-backend-batch:latest .
```

Dockerfile은 multi-stage build를 사용한다.

- builder: `eclipse-temurin:21-jdk`
- runtime: `eclipse-temurin:21-jre`
- entrypoint: `java -XX:+ExitOnOutOfMemoryError -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/app/logs -jar /app/app.jar`

## Profiles

| Profile | 용도 | 주요 특징 |
| --- | --- | --- |
| `local` | 로컬 개발 | local MariaDB/Redis/Kafka, JPA `update`, SQL log on |
| `prod` | 운영 | env 기반 DB/Redis/Kafka, JPA `update`, SQL log off |
| `remote` | 원격 개발/검증 | env 기반 DB/Redis/Kafka, JPA `validate`, SQL debug on |
| `test` | 테스트 | H2, Redis startup check off |

Docker 기본 profile은 `prod`다.

```dockerfile
ENV SPRING_PROFILES_ACTIVE=prod
```

## Required Environment Variables

운영 실행에 필요한 주요 환경변수다.

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 활성 profile | Docker: `prod`, application.yml: `local` |
| `DB_URL` | MariaDB JDBC URL | 없음 |
| `DB_USERNAME` | DB username | 없음 |
| `DB_PASSWORD` | DB password | 없음 |
| `REDIS_HOST` | Redis host | 없음 |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | empty |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers | local profile: `localhost:9092` |
| `GEMINI_API_KEY` | Gemini API key | 없음 |
| `KIS_API_KEY` 또는 `KIS_API_KEY_1` | KIS API key | empty |
| `KIS_API_SECRET` 또는 `KIS_API_SECRET_1` | KIS API secret | empty |
| `KIS_API_KEY_2` | 두 번째 KIS API key | empty |
| `KIS_API_SECRET_2` | 두 번째 KIS API secret | empty |
| `KIS_USER_ID` | KIS user id | empty |

## Optional Environment Variables

### Batch Schedule

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `BATCH_SCHEDULE_ZONE` | batch schedule timezone | `Asia/Seoul` |
| `BATCH_SCHEDULE_USER_PROFIT_SNAPSHOT_CRON` | 유저 수익률 daily snapshot cron | `0 5 * * * *` |
| `BATCH_SCHEDULE_PORTFOLIO_PERFORMANCE_SNAPSHOT_CRON` | 포트폴리오 성과 daily snapshot cron | `0 7 * * * *` |
| `BATCH_SCHEDULE_VALUATION_WRITE_BACK_CRON` | valuation write-back cron | `0/30 * * * * *` |

### Batch Size

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `BATCH_WRITE_BACK_PORTFOLIO_SIZE` | portfolio valuation dirty 처리 수 | `500` |
| `BATCH_WRITE_BACK_USER_SIZE` | user valuation dirty 처리 수 | `500` |
| `BATCH_WRITE_BACK_DELETION_SIZE` | portfolio deletion dirty 처리 수 | `500` |
| `BATCH_WARM_UP_CHUNK_SIZE` | valuation warm-up chunk size | `1000` |
| `BATCH_SNAPSHOT_USER_PROFIT_OFFSET_DAYS` | user snapshot 기준일 offset | `0` |
| `BATCH_SNAPSHOT_PORTFOLIO_PERFORMANCE_OFFSET_DAYS` | portfolio snapshot 기준일 offset | `0` |

### Manual Trigger

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `BATCH_MANUAL_TRIGGER_ENABLED` | 기동 시 수동 batch 실행 여부 | `false` |
| `BATCH_MANUAL_TRIGGER_JOBS` | 쉼표 구분 job 목록 | empty |
| `BATCH_MANUAL_TRIGGER_SNAPSHOT_DATE` | snapshot job 기준일 `yyyy-MM-dd` | empty, today |

지원 job 값:

| 값 | 동작 |
| --- | --- |
| `valuation-warm-up` | Redis valuation warm-up 실행 |
| `user-profit-snapshot` | 유저 수익률 daily snapshot 생성 |
| `portfolio-performance-snapshot` | 포트폴리오 성과 daily snapshot 생성 |
| `all` | warm-up + 두 snapshot job 실행 |

예시:

```bash
BATCH_MANUAL_TRIGGER_ENABLED=true \
BATCH_MANUAL_TRIGGER_JOBS=valuation-warm-up \
java -jar app.jar
```

### Redis Mode

Redisson 설정은 standalone/cluster를 지원한다.

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `REDIS_MODE` | `standalone` 또는 `cluster` | `standalone` |
| `REDIS_CLUSTER_NODES` | cluster nodes | empty |
| `SPRING_DATA_REDIS_CLUSTER_NODES` | Spring Redis cluster nodes | empty |

## Valuation Warm-up Runbook

Redis flush, Redis 장애 복구, worker 신규 배포 전에는 warm-up을 먼저 실행한다.

권장 순서:

1. Profit worker 또는 가격 이벤트 consumer 트래픽을 중지한다.
2. Batch 서버에서 `valuation-warm-up`을 수동 실행한다.
3. Warm-up 로그와 metric을 확인한다.
4. Profit worker 또는 가격 이벤트 consumer 트래픽을 재개한다.
5. write-back backlog와 failure metric을 확인한다.

수동 실행 예시:

```bash
SPRING_PROFILES_ACTIVE=prod \
BATCH_MANUAL_TRIGGER_ENABLED=true \
BATCH_MANUAL_TRIGGER_JOBS=valuation-warm-up \
java -jar app.jar
```

Warm-up은 현재가 의존 필드를 쓰지 않는다.

- `current-value` 미적재
- `profit-rate` 미적재
- dirty set 미등록

첫 가격 이벤트 이후 profit worker가 현재 평가액과 수익률을 계산하고 dirty set을 등록한다.

## Valuation Write-back Operation

Write-back은 기본 30초마다 실행된다.

```yaml
batch:
  schedule:
    valuation-write-back:
      cron: 0/30 * * * * *
```

Backlog가 계속 증가하면 다음 순서로 조정한다.

1. DB/Redis/Kafka 장애 여부 확인
2. `valuation.write_back.failure` 증가 여부 확인
3. batch size 증가 검토
4. cron 주기 단축 검토
5. DB 부하가 높으면 batch size 축소

## Health and Metrics

Actuator exposure:

| Endpoint | 설명 |
| --- | --- |
| `/actuator/health` | health, readiness/liveness probe |
| `/actuator/prometheus` | Prometheus metrics |
| `/actuator/info` | application info |

주요 valuation metric:

| Metric | 설명 |
| --- | --- |
| `valuation.write_back.duration` | write-back tick 처리 시간 |
| `valuation.write_back.success` | DB 반영 성공 수 |
| `valuation.write_back.failure` | DB 반영 실패 수 |
| `valuation.write_back.snapshot_missing` | Redis 필수 snapshot 누락 수 |
| `valuation.warm_up.duration` | warm-up 전체 처리 시간 |
| `valuation.warm_up.portfolio.chunks` | portfolio warm-up chunk 처리 수 |
| `valuation.warm_up.user.chunks` | user warm-up chunk 처리 수 |

## Deployment Checklist

배포 전:

- `./gradlew test` 성공 확인
- 운영 DB 스키마가 신규 read model과 호환되는지 확인
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` 설정 확인
- `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD` 설정 확인
- `KAFKA_BOOTSTRAP_SERVERS` 설정 확인
- `GEMINI_API_KEY`, KIS credential 설정 확인
- Redis mode가 standalone/cluster 중 환경과 일치하는지 확인

배포 후:

- `/actuator/health` 확인
- Redis startup check 로그 확인
- Kafka startup check 로그 확인
- write-back failure metric 확인
- warm-up이 필요한 배포라면 `valuation-warm-up` 실행 후 worker 트래픽 재개

## Notes

- 운영 `application-prod.yml`은 현재 JPA `ddl-auto: update`를 사용한다. 엄격한 schema migration이 필요하면 별도 migration 도구 도입 또는 `validate` 전환을 검토한다.
- Docker healthcheck는 JVM process 생존 여부만 확인한다. 플랫폼 readiness/liveness는 가능하면 Actuator health endpoint를 사용한다.
- batch 수동 trigger는 application startup 시 한 번 실행되는 runner다. 장기 실행 서버에서 임의 시점 실행하려면 별도 운영 절차 또는 one-shot job 형태로 실행한다.
