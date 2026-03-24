package depth.finvibe.modules.market.infra.scheduler;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.market.application.port.out.ClosingPriceRepository;
import depth.finvibe.modules.market.domain.MarketHours;
import depth.finvibe.modules.market.domain.enums.MarketStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ClosingPriceCleanupScheduler {

  private final ClosingPriceRepository closingPriceRepository;
  private MarketStatus lastMarketStatus;

  public void cleanupClosingPriceOnMarketOpen() {
    MarketStatus currentStatus = MarketHours.getCurrentStatus();

    if (lastMarketStatus != MarketStatus.OPEN && currentStatus == MarketStatus.OPEN) {
      closingPriceRepository.deleteAll();
      log.info("장이 시작되어 종가 캐시를 초기화했습니다.");
    }

    lastMarketStatus = currentStatus;
  }
}
