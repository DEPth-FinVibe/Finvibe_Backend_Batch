package depth.finvibe.modules.asset.infra.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.UserProfitSnapshotService;

@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public class UserProfitSnapshotScheduler {
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private final UserProfitSnapshotService userProfitSnapshotService;

  @Value("${batch.snapshot.user-profit.offset-days:0}")
  private long snapshotOffsetDays;

  public void saveScheduledSnapshot() {
    LocalDate snapshotDate = LocalDate.now(KST).minusDays(snapshotOffsetDays);
    userProfitSnapshotService.saveDailySnapshot(snapshotDate);
  }

  public void saveSnapshot(LocalDate snapshotDate) {
    userProfitSnapshotService.saveDailySnapshot(snapshotDate);
  }

  public void saveCurrentSnapshot() {
    saveSnapshot(LocalDate.now(KST));
  }
}
