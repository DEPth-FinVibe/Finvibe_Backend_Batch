package depth.finvibe.modules.asset.infra.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.ValuationWarmUpService;

@Slf4j
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class ValuationWarmUpScheduler {
    private final ValuationWarmUpService valuationWarmUpService;

    @SchedulerLock(name = "valuationRedisExclusive", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void executeWarmUp() {
        try {
            valuationWarmUpService.warmUp();
        } catch (Exception ex) {
            log.error("Failed to execute valuation warm-up batch", ex);
            throw ex;
        }
    }

    @SchedulerLock(name = "valuationRedisExclusive", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void executePortfolioStateWarmUp() {
        try {
            valuationWarmUpService.warmUpPortfolioStateStep();
        } catch (Exception ex) {
            log.error("Failed to execute valuation portfolio warm-up step", ex);
            throw ex;
        }
    }

    @SchedulerLock(name = "valuationRedisExclusive", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public void executeUserStateWarmUp() {
        try {
            valuationWarmUpService.warmUpUserStateStep();
        } catch (Exception ex) {
            log.error("Failed to execute valuation user warm-up step", ex);
            throw ex;
        }
    }
}
