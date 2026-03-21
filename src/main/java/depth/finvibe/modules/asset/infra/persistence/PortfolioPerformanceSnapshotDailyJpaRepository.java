package depth.finvibe.modules.asset.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import depth.finvibe.modules.asset.domain.PortfolioPerformanceSnapshotDaily;
import depth.finvibe.modules.asset.domain.PortfolioPerformanceSnapshotDailyId;

public interface PortfolioPerformanceSnapshotDailyJpaRepository
  extends JpaRepository<PortfolioPerformanceSnapshotDaily, PortfolioPerformanceSnapshotDailyId> {}
