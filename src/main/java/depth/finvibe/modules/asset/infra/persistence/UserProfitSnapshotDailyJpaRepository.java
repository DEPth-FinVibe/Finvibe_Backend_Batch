package depth.finvibe.modules.asset.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import depth.finvibe.modules.asset.domain.UserProfitSnapshotDaily;
import depth.finvibe.modules.asset.domain.UserProfitSnapshotDailyId;

public interface UserProfitSnapshotDailyJpaRepository
  extends JpaRepository<UserProfitSnapshotDaily, UserProfitSnapshotDailyId> {
  boolean existsByIdUserIdAndTotalProfitLossGreaterThanAndIdSnapshotDateLessThan(
    UUID userId,
    BigDecimal minimumProfit,
    LocalDate snapshotDate
  );
}
