package depth.finvibe.modules.asset.infra.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
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

  public void saveDailySnapshot() {
    LocalDate snapshotDate = LocalDate.now(KST).minusDays(1);
    userProfitSnapshotService.saveDailySnapshot(snapshotDate);
  }
}
