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

    if (marketStatus != MarketStatus.OPEN) {
      return;
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
