# Finvibe Backend Batch

## TODO

- Redis `portfolio:assets:{portfolioId}` writer 정리
  - 현재 Monolith는 자산/포트폴리오 이벤트 발생 시 `portfolio:assets:{portfolioId}`를 즉시 갱신하고, Batch는 warm-up/reconciliation 과정에서 DB 기준으로 같은 키를 덮어쓴다.
  - 중복 writer로 인해 책임 경계가 불명확하므로 단일 관리 주체를 정해야 한다.
  - Batch에서만 관리하려면 asset/portfolio 변경 이벤트 consumer를 Batch에 추가한 뒤 Monolith의 해당 Redis write를 제거한다.
  - 실시간성이 필요 없거나 현재 hot path에서 쓰지 않는다면 `portfolio:assets:{portfolioId}` 자체를 제거하는 방안도 검토한다.
