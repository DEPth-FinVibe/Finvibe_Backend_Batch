# 장 마감 종가 워밍업 Result

## 연결 작업

- Batch Issue: [#1](https://github.com/DEPth-FinVibe/Finvibe_Backend_Batch/issues/1)
- Monolith Issue: [#7](https://github.com/DEPth-FinVibe/Finvibe_Backend_Monolith/issues/7)
- 결정 기준: Monolith `docs/tasks/market-closed-price-reliability/03-decisions.md`

## 반영 결과

- 휴장일이 아닌 평일 15:40 KST에 전체 국내 주식 종목의 종가를 워밍업하는 Spring Batch Job을 추가했다.
- 국내 주식은 6자리 숫자 심볼로 제한해 지수용 내부 종목을 제외한다.
- KIS 클라이언트의 30종목 분할 호출을 재사용하고, 응답에서 누락된 종목만 최대 3회 다시 조회한다.
- 재시도 후에도 누락이 남으면 `IncompleteWarmUpException`을 발생시켜 Batch Job을 실패 처리한다.
- 이미 저장된 `(stock_id, trading_date)`는 다시 조회하지 않는다.
- 휴장일에는 외부 시세 호출 없이 정규 워밍업을 건너뛴다.
- 달력을 확인할 수 없으면 작업을 실패시킨다.
- 지연 기동·수동 복구를 위한 `recoverLatestCompletedClosingPrices()` application entry point를 제공한다.
- 장 시작 시 전체 삭제 작업을 제거하고 매일 16:30 KST에 30일 이전 데이터만 정리한다.

## 스케줄과 비활성화

- 워밍업: `market.closing-price.warm-up.cron` — 기본값 `0 40 15 * * MON-FRI`
- 보존 정리: `market.closing-price.cleanup.cron` — 기본값 `0 30 16 * * *`
- 특정 작업을 즉시 중지하려면 해당 cron 프로퍼티를 Spring의 disabled cron 값인 `-`로 설정한다.
- ShedLock으로 워밍업은 최대 2시간, 정리는 최대 10분의 중복 실행을 방지한다.

## 주요 지표

- `market.closing.price.warmup.target.stocks`
- `market.closing.price.warmup.succeeded.stocks`
- `market.closing.price.warmup.failed.stocks`
- `market.closing.price.warmup.attempt.failures`
- `market.closing.price.warmup.skipped{reason=closed_day}`
- `market.closing.price.warmup.last.success.epoch`
- `market.kis.bulk.price.batch.failures`
- `market.calendar.sync.failures`
- `market.calendar.status.unknown`

## 검증

- `./gradlew compileJava` 성공
- `./gradlew test` 성공
- 추가 테스트:
  - 일부 성공 후 누락 종목만 재시도
  - 휴장일 외부 호출 스킵
  - 재시도 소진 시 Job 실패용 예외와 실패 지표
- `git diff --check` 성공

## 운영 적용 및 롤백

- DB 스키마 변경은 없다.
- Monolith보다 Batch를 먼저 배포해 최근 종가를 채운 뒤 API 변경을 배포하는 순서를 권장한다.
- 문제가 생기면 두 cron 프로퍼티를 `-`로 설정해 워밍업·정리를 먼저 중지할 수 있다.
- 코드 롤백 전에 워밍업을 비활성화하면 외부 KIS 호출 증가를 즉시 차단할 수 있다.
- 무중단 배포 및 Kubernetes 설정은 요청에 따라 변경하지 않았다.
