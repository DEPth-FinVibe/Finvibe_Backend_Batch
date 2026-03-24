package depth.finvibe.modules.asset.infra.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.asset.application.port.out.UserProfitSnapshotRepository;
import depth.finvibe.modules.asset.domain.UserProfitSnapshotDaily;

@Repository
@RequiredArgsConstructor
public class UserProfitSnapshotRepositoryImpl implements UserProfitSnapshotRepository {
  private final UserProfitSnapshotDailyJpaRepository jpaRepository;
  private final EntityManager entityManager;

  @Override
  public void saveAll(List<UserProfitSnapshotDaily> snapshots) {
    if (snapshots == null || snapshots.isEmpty()) {
      return;
    }
    jpaRepository.saveAll(snapshots);
  }

  public boolean existsPositiveProfitSnapshot(UUID userId, BigDecimal minimumProfit, LocalDate beforeDate) {
    if (userId == null || minimumProfit == null || beforeDate == null) {
      return false;
    }
    return jpaRepository.existsByIdUserIdAndTotalProfitLossGreaterThanAndIdSnapshotDateLessThan(
      userId,
      minimumProfit,
      beforeDate
    );
  }

  @Override
  public Set<UUID> findUserIdsWithPositiveProfitSnapshot(
    Collection<UUID> userIds,
    BigDecimal minimumProfit,
    LocalDate beforeDate
  ) {
    if (userIds == null || userIds.isEmpty() || minimumProfit == null || beforeDate == null) {
      return Set.of();
    }

    return Set.copyOf(jpaRepository.findPositiveProfitUserIdsBeforeDate(userIds, minimumProfit, beforeDate));
  }

  @Override
  public void flushAndClear() {
    entityManager.clear();
  }
}
