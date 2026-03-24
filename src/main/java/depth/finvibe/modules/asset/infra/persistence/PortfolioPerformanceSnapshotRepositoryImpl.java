package depth.finvibe.modules.asset.infra.persistence;

import java.util.List;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.asset.application.port.out.PortfolioPerformanceSnapshotRepository;
import depth.finvibe.modules.asset.domain.PortfolioPerformanceSnapshotDaily;

@Repository
@RequiredArgsConstructor
public class PortfolioPerformanceSnapshotRepositoryImpl implements PortfolioPerformanceSnapshotRepository {
  private final PortfolioPerformanceSnapshotDailyJpaRepository jpaRepository;
  private final EntityManager entityManager;

  @Override
  public void saveAll(List<PortfolioPerformanceSnapshotDaily> snapshots) {
    if (snapshots == null || snapshots.isEmpty()) {
      return;
    }
    jpaRepository.saveAll(snapshots);
  }

  @Override
  public void flushAndClear() {
    entityManager.clear();
  }
}
