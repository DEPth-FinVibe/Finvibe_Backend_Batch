package depth.finvibe.modules.asset.infra.scheduler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import depth.finvibe.modules.asset.domain.Asset;
import depth.finvibe.modules.asset.domain.PortfolioGroup;
import depth.finvibe.modules.asset.infra.persistence.PortfolioGroupQueryRepository;
import depth.finvibe.modules.asset.infra.redis.PortfolioAssetSnapshotRedisRepository;
import depth.finvibe.modules.asset.infra.redis.PortfolioAssetSnapshotRedisRepository.AssetSnapshot;
import depth.finvibe.modules.asset.infra.redis.PortfolioOwnerRedisRepository;
import depth.finvibe.modules.asset.infra.redis.StockHoldingIndexRedisRepository;

/**
 * DB ↔ Redis 인덱스 정합성 검증 및 보정 배치.
 * 500개 단위 청킹으로 DB를 조회하여 Redis와 비교하고, 불일치 시 Redis를 DB 기준으로 보정한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisIndexReconciliationScheduler {

	private static final int CHUNK_SIZE = 500;

	private final PortfolioGroupQueryRepository portfolioGroupQueryRepository;
	private final StockHoldingIndexRedisRepository stockHoldingIndexRedisRepository;
	private final PortfolioAssetSnapshotRedisRepository portfolioAssetSnapshotRedisRepository;
	private final PortfolioOwnerRedisRepository portfolioOwnerRedisRepository;
	private final MeterRegistry meterRegistry;

	@Scheduled(fixedRate = 600_000) // 10분
	public void reconcile() {
		log.info("Redis index reconciliation started");
		long startTime = System.currentTimeMillis();
		int totalPortfolios = 0;
		int fixedCount = 0;

		Long lastPortfolioId = null;

		while (true) {
			List<Long> portfolioIds = portfolioGroupQueryRepository.findPortfolioIdsAfter(lastPortfolioId, CHUNK_SIZE);
			if (portfolioIds.isEmpty()) {
				break;
			}

			List<PortfolioGroup> portfolios = portfolioGroupQueryRepository.findAllWithAssetsByIds(portfolioIds);
			totalPortfolios += portfolios.size();

			Map<Long, Set<Long>> dbStockToPortfolios = new HashMap<>();

			for (PortfolioGroup portfolio : portfolios) {
				fixedCount += reconcilePortfolio(portfolio, dbStockToPortfolios);
			}

			for (Map.Entry<Long, Set<Long>> entry : dbStockToPortfolios.entrySet()) {
				Long stockId = entry.getKey();
				Set<Long> dbPortfolioIds = entry.getValue();
				Set<Long> redisPortfolioIds = stockHoldingIndexRedisRepository.getPortfolioIds(stockId);

				if (!dbPortfolioIds.equals(redisPortfolioIds)) {
					Set<Long> merged = new HashSet<>(redisPortfolioIds);
					merged.addAll(dbPortfolioIds);

					Set<Long> onlyInRedis = new HashSet<>(redisPortfolioIds);
					onlyInRedis.removeAll(dbPortfolioIds);

					if (!onlyInRedis.isEmpty()) {
						// 이 청크에 포함되지 않은 포트폴리오일 수 있으므로 추가만 수행
					}

					for (Long pid : dbPortfolioIds) {
						if (!redisPortfolioIds.contains(pid)) {
							stockHoldingIndexRedisRepository.replacePortfolios(stockId, merged);
							fixedCount++;
							break;
						}
					}
				}
			}

			lastPortfolioId = portfolioIds.get(portfolioIds.size() - 1);
		}

		long elapsed = System.currentTimeMillis() - startTime;
		meterRegistry.counter("reconciliation.runs").increment();
		meterRegistry.counter("reconciliation.fixes").increment(fixedCount);

		log.info("Redis index reconciliation completed: {} portfolios checked, {} fixes applied, {}ms elapsed",
				totalPortfolios, fixedCount, elapsed);
	}

	private int reconcilePortfolio(PortfolioGroup portfolio, Map<Long, Set<Long>> dbStockToPortfolios) {
		int fixes = 0;
		Long portfolioId = portfolio.getId();

		// 1. portfolio:owner 검증
		java.util.UUID redisOwner = portfolioOwnerRedisRepository.get(portfolioId);
		if (!portfolio.getUserId().equals(redisOwner)) {
			portfolioOwnerRedisRepository.set(portfolioId, portfolio.getUserId());
			fixes++;
		}

		// 2. portfolio:assets 검증
		Map<Long, AssetSnapshot> dbSnapshots = portfolio.getAssets().stream()
				.collect(Collectors.toMap(
						Asset::getStockId,
						asset -> new AssetSnapshot(
								asset.getAmount(),
								asset.getTotalPrice().getAmount(),
								asset.getTotalPrice().getCurrency().name()
						),
						(a, b) -> a
				));

		Map<Long, AssetSnapshot> redisSnapshots = portfolioAssetSnapshotRedisRepository.getAssets(portfolioId);

		if (!dbSnapshots.equals(redisSnapshots)) {
			portfolioAssetSnapshotRedisRepository.replaceAll(portfolioId, dbSnapshots);
			fixes++;
		}

		// 3. stock:holding 역방향 인덱스 수집
		for (Asset asset : portfolio.getAssets()) {
			dbStockToPortfolios
					.computeIfAbsent(asset.getStockId(), k -> new HashSet<>())
					.add(portfolioId);
		}

		return fixes;
	}
}
