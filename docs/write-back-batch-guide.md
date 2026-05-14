# Valuation Write-back Batch Guide

이 문서는 profit worker가 Redis에 저장한 valuation snapshot과 dirty marker를 batch 서버가 DB에 반영하는 방법, 그리고 Redis 장애/초기 기동 시 필요한 warm-up 범위를 정리한다.

현재 구현 기준 클래스:

- `ValuationWriteBackService`
- `ValuationWarmUpService`
- `ValuationSnapshotRedisRepository`
- `ValuationDirtyRedisRepository`
- `ValuationWarmUpRedisRepositoryImpl`

## 책임

| 책임 | 설명 |
| --- | --- |
| Redis warm-up | worker가 가격 이벤트를 처리할 수 있도록 가격 비의존 Redis 상태를 미리 적재한다. |
| Dirty 대상 조회 | Redis dirty set에서 write-back 대상 portfolio/user를 조회한다. |
| Snapshot 읽기 | 대상 ID별 Redis valuation snapshot을 읽는다. |
| DB upsert | 정상 dirty 대상은 DB valuation read model에 upsert 한다. |
| DB soft delete | 포트폴리오 삭제 dirty 대상은 DB portfolio valuation row를 soft delete 한다. |
| Dirty 제거 | DB 반영에 성공한 대상만 Redis dirty set에서 제거한다. |
| 재시도 보장 | 실패한 대상은 dirty set에 남겨 다음 batch tick에서 재처리한다. |

## Warm-up 정책

Warm-up은 **현재가에 의존하지 않는 상태만** Redis에 적재한다. 현재가, 현재 평가액, 수익률은 첫 가격 이벤트가 발생한 뒤 profit worker가 계산한다.

### 포함 대상

| Redis key | 타입 | 설명 |
| --- | --- | --- |
| `stock:holding:{stockId}:portfolios` | Set<Long> | 모놀리스 호환 종목 -> 포트폴리오 인덱스 |
| `portfolio:assets:{portfolioId}` | Hash | 모놀리스 호환 포트폴리오 보유 종목 snapshot |
| `portfolio:owner:{portfolioId}` | String | 모놀리스 호환 포트폴리오 소유자 |
| `stock:{stockId}:portfolios` | Set<Long> | 가이드 신규 종목 -> 포트폴리오 인덱스 |
| `portfolio:{portfolioId}:stocks` | Set<Long> | 포트폴리오 보유 종목 목록 |
| `portfolio:{portfolioId}:stock:{stockId}:quantity` | String | 해당 종목 보유 수량 |
| `portfolio:{portfolioId}:user` | String | 포트폴리오 소유자 userId |
| `user:{userId}:portfolios` | Set<Long> | 유저 활성 포트폴리오 목록 |
| `portfolio:{portfolioId}:purchased-value` | String(Long) | 포트폴리오 총 매입금 |
| `portfolio:{portfolioId}:asset-count` | String(Long) | 보유 수량이 0보다 큰 종목 수 |
| `portfolio:{portfolioId}:deleted` | String(Boolean) | warm-up 대상은 `false` |
| `portfolio:{portfolioId}:updated-at` | Instant string | warm-up 적재 시각 |
| `user:{userId}:purchased-value` | String(Long) | 유저 전체 총 매입금 |
| `user:{userId}:portfolio-count` | String(Long) | 유저 활성 포트폴리오 수 |
| `user:{userId}:updated-at` | Instant string | warm-up 적재 시각 |

### 제외 대상

| Redis key 또는 동작 | 제외 이유 |
| --- | --- |
| `portfolio:{portfolioId}:stock:{stockId}:current-value` | 현재가 필요 |
| `portfolio:{portfolioId}:current-value` | 현재가 필요 |
| `portfolio:{portfolioId}:profit-rate` | 현재 평가액 필요 |
| `user:{userId}:current-value` | 현재 평가액 필요 |
| `user:{userId}:profit-rate` | 현재 평가액 필요 |
| `dirty:portfolio-valuations` 등록 | 필수 snapshot이 완성되지 않았으므로 write-back 불가 |
| `dirty:user-valuations` 등록 | 필수 snapshot이 완성되지 않았으므로 write-back 불가 |

## Warm-up Source

Warm-up source는 DB의 자산 원천 모델이다.

| 목적 | 엔티티 | 필드 |
| --- | --- | --- |
| 활성 포트폴리오 | `PortfolioGroup` | `id`, `userId` |
| 보유 종목 | `Asset` | `stockId`, `amount`, `totalPrice.amount`, `totalPrice.currency` |
| 포트폴리오-유저 관계 | `PortfolioGroup` | `userId` |

현재 asset 도메인에는 portfolio soft delete 컬럼이 없다. 따라서 DB에 존재하는 `PortfolioGroup`은 warm-up 기준 활성 포트폴리오로 본다.

보유 종목은 `asset.amount > 0`인 경우만 포함한다.

## Warm-up 대용량 처리

유저 수가 수십만 이상일 수 있으므로 전체 데이터를 한 번에 로딩하지 않는다.

현재 구현은 Spring Batch job `executeValuationWarmUpJob`에서 2-step으로 수행한다.

| Step | 설명 |
| --- | --- |
| `portfolioStateWarmUpStep` | `portfolio.id > lastId` keyset chunk로 portfolio/asset 상태 적재 |
| `userStateWarmUpStep` | `userId > lastUserId` keyset chunk로 user aggregate 상태 적재 |

OOM 방지 원칙:

- `findAllWithAssets()` 전체 조회 금지
- offset paging 금지
- keyset paging 사용
- 기본 chunk size는 `1000`
- Redis write는 chunk 단위 pipeline 사용
- chunk 처리 후 `EntityManager.clear()` 호출
- JVM에 전체 user aggregate map을 보관하지 않음

설정:

```yaml
batch:
  warm-up:
    chunk-size: ${BATCH_WARM_UP_CHUNK_SIZE:1000}
```

수동 실행:

```text
BATCH_MANUAL_TRIGGER_ENABLED=true
BATCH_MANUAL_TRIGGER_JOBS=valuation-warm-up
```

## Write-back Dirty Sets

| Redis set | 값 타입 | 처리 |
| --- | --- | --- |
| `dirty:portfolio-valuation-deletions` | `Long` portfolioId | portfolio valuation soft delete |
| `dirty:portfolio-valuations` | `Long` portfolioId | portfolio valuation upsert |
| `dirty:user-valuations` | `String` userId | user valuation upsert |

처리 원칙:

- dirty set은 queue처럼 취급하되 실패 시 유실되면 안 된다.
- `SPOP`은 사용하지 않는다.
- `SSCAN`으로 읽고 DB 성공 후 `SREM` 한다.
- 실패하거나 필수 snapshot이 누락되면 dirty set에 남긴다.

## Write-back Snapshot Keys

### Portfolio Valuation

| 필드 | Redis key | 타입 | 필수 |
| --- | --- | --- | --- |
| purchasedValue | `portfolio:{portfolioId}:purchased-value` | Long | 필수 |
| currentValue | `portfolio:{portfolioId}:current-value` | Long | 필수 |
| profitRate | `portfolio:{portfolioId}:profit-rate` | Double | 필수 |
| assetCount | `portfolio:{portfolioId}:asset-count` | Long | 필수 |
| updatedAt | `portfolio:{portfolioId}:updated-at` | Instant string | 권장 |
| deleted | `portfolio:{portfolioId}:deleted` | Boolean string | 삭제 판단 |
| deletedAt | `portfolio:{portfolioId}:deleted-at` | Instant string | 삭제 처리 필수 |

### User Valuation

| 필드 | Redis key | 타입 | 필수 |
| --- | --- | --- | --- |
| purchasedValue | `user:{userId}:purchased-value` | Long | 필수 |
| currentValue | `user:{userId}:current-value` | Long | 필수 |
| profitRate | `user:{userId}:profit-rate` | Double | 필수 |
| portfolioCount | `user:{userId}:portfolio-count` | Long | 필수 |
| updatedAt | `user:{userId}:updated-at` | Instant string | 권장 |

Warm-up 직후에는 `current-value`, `profit-rate`가 없을 수 있다. 이 상태에서는 worker가 dirty set에 넣지 않아야 하며, write-back batch도 dirty 대상이 없으므로 DB 반영을 시도하지 않는다.

## 처리 순서

한 번의 write-back tick에서는 다음 순서를 지킨다.

1. Portfolio deletion dirty 처리
2. Portfolio valuation dirty 처리
3. User valuation dirty 처리

삭제를 먼저 처리해야 삭제된 포트폴리오가 일반 upsert로 다시 살아나는 것을 방지할 수 있다.

## DB Read Model

### `portfolio_valuation`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `portfolio_id` | BIGINT | PK |
| `purchased_value` | BIGINT | 총 매입금 |
| `current_value` | BIGINT | 현재 평가액 |
| `profit_rate` | DOUBLE | 수익률 |
| `asset_count` | BIGINT | 보유 종목 수 |
| `updated_at` | TIMESTAMP | 마지막 snapshot 시각 |
| `deleted` | BOOLEAN | soft delete 여부 |
| `deleted_at` | TIMESTAMP | soft delete 시각 |

### `user_valuation`

| 컬럼 | 타입 | 설명 |
| --- | --- | --- |
| `user_id` | VARCHAR | PK. worker 기준 String userId |
| `purchased_value` | BIGINT | 총 매입금 |
| `current_value` | BIGINT | 현재 평가액 |
| `profit_rate` | DOUBLE | 수익률 |
| `portfolio_count` | BIGINT | 포트폴리오 수 |
| `updated_at` | TIMESTAMP | 마지막 snapshot 시각 |

## 최신성 정책

- 일반 upsert는 Redis `updatedAt`이 DB `updatedAt`보다 최신일 때만 반영한다.
- DB `updatedAt`이 없으면 Redis snapshot을 반영한다.
- soft delete는 DB `deletedAt`이 없거나 Redis `deletedAt`이 더 최신일 때 반영한다.
- soft delete된 portfolio valuation은 일반 upsert로 되살리지 않는다.

## Dirty 제거 기준

| 상황 | 처리 |
| --- | --- |
| DB upsert 성공 | 해당 dirty set에서 대상 제거 |
| DB soft delete 성공 | deletion dirty 제거, 필요 시 portfolio dirty도 제거 |
| DB 반영 실패 | dirty set 유지 |
| Redis 필수 필드 누락 | dirty set 유지 |
| Redis deletedAt 누락 | deletion dirty 유지 |

## 운영 설정

```yaml
batch:
  schedule:
    valuation-write-back:
      cron: ${BATCH_SCHEDULE_VALUATION_WRITE_BACK_CRON:0/30 * * * * *}
  write-back:
    portfolio-size: ${BATCH_WRITE_BACK_PORTFOLIO_SIZE:500}
    user-size: ${BATCH_WRITE_BACK_USER_SIZE:500}
    deletion-size: ${BATCH_WRITE_BACK_DELETION_SIZE:500}
  warm-up:
    chunk-size: ${BATCH_WARM_UP_CHUNK_SIZE:1000}
```

운영 기본 write-back 주기는 30초다. backlog가 증가하면 batch size 또는 cron 주기를 조정한다.

## Metric

현재 노출되는 주요 metric:

| Metric | 설명 |
| --- | --- |
| `valuation.write_back.duration` | write-back tick 처리 시간 |
| `valuation.write_back.success` | write-back 성공 수 |
| `valuation.write_back.failure` | write-back 실패 수 |
| `valuation.write_back.snapshot_missing` | 필수 snapshot 누락 수 |
| `valuation.warm_up.duration` | warm-up 전체 처리 시간 |
| `valuation.warm_up.portfolio.chunks` | portfolio warm-up chunk 처리 수 |
| `valuation.warm_up.user.chunks` | user warm-up chunk 처리 수 |

## 주의 사항

- Warm-up은 dirty set을 등록하지 않는다.
- Warm-up은 현재가 의존 필드를 쓰지 않는다.
- Redis 필드 값은 문자열로 저장되므로 write-back에서 Long, Double, Boolean, Instant로 변환한다.
- Redis 장애 또는 flush 이후에는 `valuation-warm-up`을 먼저 실행한 뒤 worker 트래픽을 정상화한다.
- `userId`는 worker 기준 String이다. daily snapshot 저장 시 내부 Long userId가 필요한 경우 `UserIdResolver`를 통해 변환한다.
