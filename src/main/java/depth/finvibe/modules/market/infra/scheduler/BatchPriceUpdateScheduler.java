package depth.finvibe.modules.market.infra.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.market.application.BatchPriceUpdateService;
import depth.finvibe.modules.market.domain.MarketHours;
import depth.finvibe.modules.market.domain.enums.MarketStatus;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class BatchPriceUpdateScheduler {

  private final BatchPriceUpdateService batchPriceUpdateService;
  private MarketStatus lastMarketStatus;

  public void executeBatchPriceUpdate() {
    MarketStatus marketStatus = MarketHours.getCurrentStatus();
    logMarketStatusTransition(marketStatus);

    // 장중이 아니어도 캐시가 비었으면 갱신한다.
    // 현재가 캐시는 TTL이 24시간인데 갱신은 장중에만 돌아서, 주말·연휴처럼 장이 오래 닫히면
    // TTL이 먼저 끝나 카테고리 종목 조회가 통째로 실패한다. 그 구멍을 배치가 직접 메운다.
    if (marketStatus != MarketStatus.OPEN) {
      if (!batchPriceUpdateService.hasMissingBatchPricesByKey()) {
        return;
      }
      log.info("장이 닫혀 있지만 현재가 캐시가 비어 있어 갱신을 진행합니다. 상태: {}", marketStatus);
    }

    log.debug("Starting scheduled batch price update");
    try {
      batchPriceUpdateService.updateHoldingStockPrices();
      log.debug("Completed scheduled batch price update");
    } catch (Exception e) {
      log.error("Failed to execute batch price update", e);
      throw e;
    }
  }

  private void logMarketStatusTransition(MarketStatus currentStatus) {
    if (currentStatus == lastMarketStatus) {
      return;
    }

    if (currentStatus == MarketStatus.OPEN) {
      log.info("장 상태가 OPEN으로 전환되어 시세 배치 업데이트를 재개합니다.");
    } else {
      log.info("장 상태가 OPEN이 아니어서 시세 배치 업데이트를 대기합니다. 상태: {}", currentStatus);
    }
    lastMarketStatus = currentStatus;
  }

}
