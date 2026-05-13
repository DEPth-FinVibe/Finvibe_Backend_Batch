package depth.finvibe.modules.asset.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.modules.asset.application.port.out.PortfolioPerformanceSnapshotRepository;
import depth.finvibe.modules.asset.domain.PortfolioGroup;
import depth.finvibe.modules.asset.domain.PortfolioPerformanceSnapshotDaily;
import depth.finvibe.modules.asset.domain.PortfolioPerformanceSnapshotDailyId;
import depth.finvibe.modules.asset.domain.PortfolioValuation;
import depth.finvibe.modules.user.application.service.UserIdResolver;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioPerformanceSnapshotService {
  private final PortfolioGroupRepository portfolioGroupRepository;
  private final PortfolioPerformanceSnapshotRepository portfolioPerformanceSnapshotRepository;
  private final UserIdResolver userIdResolver;

  @Value("${batch.chunk.portfolio-snapshot-size:500}")
  private int portfolioChunkSize;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void saveDailySnapshot(LocalDate snapshotDate) {
    if (snapshotDate == null) {
      return;
    }

    Long lastPortfolioId = null;
    while (true) {
      List<Long> portfolioIds = portfolioGroupRepository.findPortfolioIdsAfter(lastPortfolioId, portfolioChunkSize);
      if (portfolioIds.isEmpty()) {
        return;
      }

      List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllWithAssetsByIds(portfolioIds);
      saveSnapshotChunk(portfolios, snapshotDate);

      lastPortfolioId = portfolioIds.get(portfolioIds.size() - 1);
      portfolioPerformanceSnapshotRepository.flushAndClear();
    }
  }

  private void saveSnapshotChunk(List<PortfolioGroup> portfolios, LocalDate snapshotDate) {
    List<PortfolioPerformanceSnapshotDaily> snapshots = portfolios.stream()
      .filter(portfolio -> portfolio.getId() != null)
      .map(portfolio -> toSnapshotIfResolvable(portfolio, snapshotDate))
      .flatMap(Optional::stream)
      .toList();

    portfolioPerformanceSnapshotRepository.saveAll(snapshots);
  }

  private Optional<PortfolioPerformanceSnapshotDaily> toSnapshotIfResolvable(PortfolioGroup portfolio, LocalDate snapshotDate) {
    Optional<Long> internalUserId = userIdResolver.resolveInternalUserIdIfPresent(portfolio.getUserId());
    if (internalUserId.isEmpty()) {
      log.warn("Skip portfolio performance snapshot for unresolved user. portfolioId={}, userId={}, snapshotDate={}",
        portfolio.getId(), portfolio.getUserId(), snapshotDate);
      return Optional.empty();
    }

    PortfolioValuation valuation = portfolio.getValuation();
    BigDecimal totalCurrentValue = BigDecimal.ZERO;
    BigDecimal totalProfitLoss = BigDecimal.ZERO;
    BigDecimal totalReturnRate = BigDecimal.ZERO;

    if (valuation != null) {
      totalCurrentValue = valuation.getTotalCurrentValue() != null ? valuation.getTotalCurrentValue() : BigDecimal.ZERO;
      totalProfitLoss = valuation.getTotalProfitLoss() != null ? valuation.getTotalProfitLoss() : BigDecimal.ZERO;
      totalReturnRate = valuation.getTotalReturnRate() != null ? valuation.getTotalReturnRate() : BigDecimal.ZERO;
    }

    return Optional.of(PortfolioPerformanceSnapshotDaily.create(
      new PortfolioPerformanceSnapshotDailyId(portfolio.getId(), snapshotDate),
      internalUserId.get(),
      portfolio.getName(),
      totalCurrentValue,
      totalProfitLoss,
      totalReturnRate
    ));
  }
}
