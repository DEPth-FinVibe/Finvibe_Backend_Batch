package depth.finvibe.modules.market.infra.scheduler;

import depth.finvibe.modules.market.application.ClosingPriceWarmUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ClosingPriceWarmUpScheduler {

  private final ClosingPriceWarmUpService closingPriceWarmUpService;

  public void warmUpClosingPrices() {
    closingPriceWarmUpService.warmUpScheduledClosingPrices();
  }
}
