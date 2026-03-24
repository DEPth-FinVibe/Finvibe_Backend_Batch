package depth.finvibe.modules.asset.infra.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.PortfolioPerformanceSnapshotService;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class PortfolioPerformanceSnapshotScheduler {
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final PortfolioPerformanceSnapshotService portfolioPerformanceSnapshotService;

  public void saveDailySnapshot() {
    LocalDate snapshotDate = LocalDate.now(KST).minusDays(1);
    portfolioPerformanceSnapshotService.saveDailySnapshot(snapshotDate);
  }
}
