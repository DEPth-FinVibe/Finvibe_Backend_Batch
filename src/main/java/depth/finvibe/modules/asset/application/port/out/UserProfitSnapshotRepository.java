package depth.finvibe.modules.asset.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import depth.finvibe.modules.asset.domain.UserProfitSnapshotDaily;

public interface UserProfitSnapshotRepository {
  void saveAll(List<UserProfitSnapshotDaily> snapshots);

  boolean existsPositiveProfitSnapshot(UUID userId, BigDecimal minimumProfit, LocalDate beforeDate);

  Set<UUID> findUserIdsWithPositiveProfitSnapshot(
    Collection<UUID> userIds,
    BigDecimal minimumProfit,
    LocalDate beforeDate
  );

  void flushAndClear();
}
