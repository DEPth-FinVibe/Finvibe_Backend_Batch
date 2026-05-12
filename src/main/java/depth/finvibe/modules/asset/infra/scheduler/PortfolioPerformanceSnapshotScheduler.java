package depth.finvibe.modules.asset.infra.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${batch.snapshot.portfolio-performance.offset-days:0}")
  private long snapshotOffsetDays;

  public void saveScheduledSnapshot() {
    LocalDate snapshotDate = LocalDate.now(KST).minusDays(snapshotOffsetDays);
    portfolioPerformanceSnapshotService.saveDailySnapshot(snapshotDate);
  }

  public void saveSnapshot(LocalDate snapshotDate) {
    portfolioPerformanceSnapshotService.saveDailySnapshot(snapshotDate);
  }

  public void saveCurrentSnapshot() {
    saveSnapshot(LocalDate.now(KST));
  }
}
