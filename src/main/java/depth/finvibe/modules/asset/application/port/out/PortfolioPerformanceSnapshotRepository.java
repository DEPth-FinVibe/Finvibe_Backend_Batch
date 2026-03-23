package depth.finvibe.modules.asset.application.port.out;

import java.util.List;

import depth.finvibe.modules.asset.domain.PortfolioPerformanceSnapshotDaily;

public interface PortfolioPerformanceSnapshotRepository {
  void saveAll(List<PortfolioPerformanceSnapshotDaily> snapshots);

  void flushAndClear();
}
