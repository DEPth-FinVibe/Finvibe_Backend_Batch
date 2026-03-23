package depth.finvibe.modules.asset.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import depth.finvibe.modules.asset.domain.UserProfitSnapshotDaily;
import depth.finvibe.modules.asset.domain.UserProfitSnapshotDailyId;

public interface UserProfitSnapshotDailyJpaRepository
  extends JpaRepository<UserProfitSnapshotDaily, UserProfitSnapshotDailyId> {
  boolean existsByIdUserIdAndTotalProfitLossGreaterThanAndIdSnapshotDateLessThan(
    UUID userId,
    BigDecimal minimumProfit,
    LocalDate snapshotDate
  );

  @Query("""
    select distinct snapshot.id.userId
    from UserProfitSnapshotDaily snapshot
    where snapshot.id.userId in :userIds
      and snapshot.totalProfitLoss > :minimumProfit
      and snapshot.id.snapshotDate < :snapshotDate
    """)
  List<UUID> findPositiveProfitUserIdsBeforeDate(
    @Param("userIds") Collection<UUID> userIds,
    @Param("minimumProfit") BigDecimal minimumProfit,
    @Param("snapshotDate") LocalDate snapshotDate
  );
}
