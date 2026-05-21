package depth.finvibe.modules.asset.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.modules.asset.application.port.out.PortfolioPerformanceSnapshotRepository;
import depth.finvibe.modules.asset.application.port.out.PortfolioValuationRepository;
import depth.finvibe.modules.asset.domain.PortfolioGroup;
import depth.finvibe.modules.asset.domain.PortfolioPerformanceSnapshotDaily;
import depth.finvibe.modules.asset.domain.PortfolioPerformanceSnapshotDailyId;
import depth.finvibe.modules.asset.domain.PortfolioValuationReadModel;
import depth.finvibe.modules.user.application.service.UserIdResolver;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioPerformanceSnapshotService {
  private final PortfolioGroupRepository portfolioGroupRepository;
  private final PortfolioPerformanceSnapshotRepository portfolioPerformanceSnapshotRepository;
  private final PortfolioValuationRepository portfolioValuationRepository;
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
      List<Long> portfolioIds = portfolioValuationRepository.findActivePortfolioIdsAfter(lastPortfolioId, portfolioChunkSize);
      if (portfolioIds.isEmpty()) {
        return;
      }

      saveSnapshotChunk(portfolioIds, snapshotDate);

      lastPortfolioId = portfolioIds.get(portfolioIds.size() - 1);
      portfolioPerformanceSnapshotRepository.flushAndClear();
    }
  }

  private void saveSnapshotChunk(List<Long> portfolioIds, LocalDate snapshotDate) {
    Map<Long, PortfolioValuationReadModel> valuationsByPortfolioId = portfolioValuationRepository.findActiveByPortfolioIds(portfolioIds)
      .stream()
      .collect(Collectors.toMap(PortfolioValuationReadModel::getPortfolioId, Function.identity()));
    List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllWithAssetsByIds(portfolioIds);

    List<PortfolioPerformanceSnapshotDaily> snapshots = portfolios.stream()
      .filter(portfolio -> portfolio.getId() != null)
      .map(portfolio -> toSnapshotIfResolvable(portfolio, valuationsByPortfolioId.get(portfolio.getId()), snapshotDate))
      .flatMap(Optional::stream)
      .toList();

    portfolioPerformanceSnapshotRepository.saveAll(snapshots);
  }

  private Optional<PortfolioPerformanceSnapshotDaily> toSnapshotIfResolvable(
    PortfolioGroup portfolio,
    PortfolioValuationReadModel valuation,
    LocalDate snapshotDate
  ) {
    if (valuation == null) {
      return Optional.empty();
    }

    Long internalUserId = portfolio.getUserId();
    if (internalUserId == null) {
      log.warn("Skip portfolio performance snapshot for unresolved user. portfolioId={}, userId={}, snapshotDate={}",
        portfolio.getId(), portfolio.getUserId(), snapshotDate);
      return Optional.empty();
    }

    BigDecimal totalCurrentValue = BigDecimal.valueOf(valuation.getCurrentValue());
    BigDecimal totalProfitLoss = totalCurrentValue.subtract(BigDecimal.valueOf(valuation.getPurchasedValue()));
    BigDecimal totalReturnRate = BigDecimal.valueOf(valuation.getProfitRate());

    return Optional.of(PortfolioPerformanceSnapshotDaily.create(
      new PortfolioPerformanceSnapshotDailyId(portfolio.getId(), snapshotDate),
      internalUserId,
      portfolio.getName(),
      totalCurrentValue,
      totalProfitLoss,
      totalReturnRate
    ));
  }
}
