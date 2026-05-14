package depth.finvibe.modules.asset.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.dto.PortfolioWarmUpState;
import depth.finvibe.modules.asset.application.dto.UserWarmUpState;
import depth.finvibe.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.modules.asset.application.port.out.ValuationDirtyRepository;
import depth.finvibe.modules.asset.application.port.out.ValuationWarmUpRedisRepository;
import depth.finvibe.modules.asset.domain.Asset;
import depth.finvibe.modules.asset.domain.PortfolioGroup;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValuationWarmUpService {
    private static final BigDecimal POSITIVE_AMOUNT_THRESHOLD = BigDecimal.ZERO;

    private final PortfolioGroupRepository portfolioGroupRepository;
    private final ValuationWarmUpRedisRepository valuationWarmUpRedisRepository;
    private final ValuationDirtyRepository valuationDirtyRepository;
    private final EntityManager entityManager;
    private final MeterRegistry meterRegistry;

    @Value("${batch.warm-up.chunk-size:1000}")
    private int chunkSize;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void warmUp() {
        Instant startedAt = Instant.now();
        log.info("Valuation warm-up started. chunkSize={}", chunkSize);
        WarmUpResult portfolioResult = warmUpPortfolioStates();
        WarmUpResult userResult = warmUpUserStates();
        Duration elapsed = Duration.between(startedAt, Instant.now());
        meterRegistry.timer("valuation.warm_up.duration")
            .record(elapsed);
        log.info(
            "Valuation warm-up completed. portfolios={}, users={}, holdings={}, elapsedMs={}",
            portfolioResult.portfolioCount(),
            userResult.userCount(),
            portfolioResult.holdingCount(),
            elapsed.toMillis()
        );
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void warmUpPortfolioStateStep() {
        log.info("Valuation portfolio warm-up step started. chunkSize={}", chunkSize);
        WarmUpResult result = warmUpPortfolioStates();
        log.info("Valuation portfolio warm-up step completed. portfolios={}, holdings={}", result.portfolioCount(), result.holdingCount());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void warmUpUserStateStep() {
        log.info("Valuation user warm-up step started. chunkSize={}", chunkSize);
        WarmUpResult result = warmUpUserStates();
        log.info("Valuation user warm-up step completed. users={}", result.userCount());
    }

    private WarmUpResult warmUpPortfolioStates() {
        Long lastPortfolioId = null;
        long portfolioCount = 0L;
        long holdingCount = 0L;
        long chunkCount = 0L;
        Instant phaseStartedAt = Instant.now();

        log.info("Valuation portfolio warm-up started. chunkSize={}", chunkSize);

        while (true) {
            List<Long> portfolioIds = portfolioGroupRepository.findPortfolioIdsAfter(lastPortfolioId, chunkSize);
            if (portfolioIds.isEmpty()) {
                log.info(
                    "Valuation portfolio warm-up completed. chunks={}, portfolios={}, holdings={}, elapsedMs={}",
                    chunkCount,
                    portfolioCount,
                    holdingCount,
                    Duration.between(phaseStartedAt, Instant.now()).toMillis()
                );
                return new WarmUpResult(portfolioCount, 0L, holdingCount);
            }

            Instant chunkStartedAt = Instant.now();
            List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllWithAssetsByIds(portfolioIds);
            Instant updatedAt = Instant.now();
            List<PortfolioWarmUpState> states = portfolios.stream()
                .filter(portfolio -> portfolio.getId() != null && portfolio.getUserId() != null)
                .map(portfolio -> toPortfolioWarmUpState(portfolio, updatedAt))
                .toList();

            valuationWarmUpRedisRepository.writePortfolioStates(states);
            valuationDirtyRepository.addPortfolioValuationDirty(states.stream()
                .map(PortfolioWarmUpState::portfolioId)
                .toList());
            portfolioCount += states.size();
            holdingCount += states.stream()
                .mapToLong(state -> state.holdings().size())
                .sum();
            chunkCount++;

            lastPortfolioId = portfolioIds.get(portfolioIds.size() - 1);
            meterRegistry.counter("valuation.warm_up.portfolio.chunks").increment();
            log.info(
                "Valuation portfolio warm-up chunk completed. chunk={}, requestedPortfolios={}, writtenPortfolios={}, writtenHoldings={}, totalPortfolios={}, totalHoldings={}, lastPortfolioId={}, elapsedMs={}",
                chunkCount,
                portfolioIds.size(),
                states.size(),
                states.stream().mapToLong(state -> state.holdings().size()).sum(),
                portfolioCount,
                holdingCount,
                lastPortfolioId,
                Duration.between(chunkStartedAt, Instant.now()).toMillis()
            );
            entityManager.clear();
        }
    }

    private WarmUpResult warmUpUserStates() {
        UUID lastUserId = null;
        long userCount = 0L;
        long chunkCount = 0L;
        Instant phaseStartedAt = Instant.now();

        log.info("Valuation user warm-up started. chunkSize={}", chunkSize);

        while (true) {
            List<UUID> userIds = portfolioGroupRepository.findUserIdsAfter(lastUserId, chunkSize);
            if (userIds.isEmpty()) {
                log.info(
                    "Valuation user warm-up completed. chunks={}, users={}, elapsedMs={}",
                    chunkCount,
                    userCount,
                    Duration.between(phaseStartedAt, Instant.now()).toMillis()
                );
                return new WarmUpResult(0L, userCount, 0L);
            }

            Instant chunkStartedAt = Instant.now();
            List<PortfolioGroup> portfolios = portfolioGroupRepository.findAllWithAssetsByUserIds(userIds);
            Instant updatedAt = Instant.now();
            List<UserWarmUpState> states = toUserWarmUpStates(portfolios, updatedAt);

            valuationWarmUpRedisRepository.writeUserStates(states);
            valuationDirtyRepository.addUserValuationDirty(states.stream()
                .map(UserWarmUpState::userId)
                .toList());
            userCount += states.size();
            chunkCount++;

            lastUserId = userIds.get(userIds.size() - 1);
            meterRegistry.counter("valuation.warm_up.user.chunks").increment();
            log.info(
                "Valuation user warm-up chunk completed. chunk={}, requestedUsers={}, writtenUsers={}, loadedPortfolios={}, totalUsers={}, lastUserId={}, elapsedMs={}",
                chunkCount,
                userIds.size(),
                states.size(),
                portfolios.size(),
                userCount,
                lastUserId,
                Duration.between(chunkStartedAt, Instant.now()).toMillis()
            );
            entityManager.clear();
        }
    }

    private PortfolioWarmUpState toPortfolioWarmUpState(PortfolioGroup portfolio, Instant updatedAt) {
        List<PortfolioWarmUpState.Holding> holdings = portfolio.getAssets().stream()
            .filter(this::hasPositiveAmount)
            .map(asset -> new PortfolioWarmUpState.Holding(
                asset.getStockId(),
                asset.getAmount(),
                asset.getTotalPrice().getAmount(),
                asset.getTotalPrice().getCurrency().name()
            ))
            .toList();

        long purchasedValue = holdings.stream()
            .map(PortfolioWarmUpState.Holding::purchasePriceAmount)
            .mapToLong(this::toLongAmount)
            .sum();

        return new PortfolioWarmUpState(
            portfolio.getId(),
            portfolio.getUserId().toString(),
            purchasedValue,
            purchasedValue,
            0D,
            (long) holdings.size(),
            updatedAt,
            holdings
        );
    }

    private List<UserWarmUpState> toUserWarmUpStates(List<PortfolioGroup> portfolios, Instant updatedAt) {
        Map<UUID, List<PortfolioGroup>> portfoliosByUserId = portfolios.stream()
            .filter(portfolio -> portfolio.getUserId() != null)
            .collect(Collectors.groupingBy(PortfolioGroup::getUserId));

        List<UserWarmUpState> states = new ArrayList<>(portfoliosByUserId.size());
        for (Map.Entry<UUID, List<PortfolioGroup>> entry : portfoliosByUserId.entrySet()) {
            List<PortfolioGroup> userPortfolios = entry.getValue();
            long purchasedValue = userPortfolios.stream()
                .flatMap(portfolio -> portfolio.getAssets().stream())
                .filter(this::hasPositiveAmount)
                .map(asset -> asset.getTotalPrice().getAmount())
                .mapToLong(this::toLongAmount)
                .sum();
            List<Long> portfolioIds = userPortfolios.stream()
                .map(PortfolioGroup::getId)
                .filter(Objects::nonNull)
                .sorted()
                .toList();

            states.add(new UserWarmUpState(
                entry.getKey().toString(),
                purchasedValue,
                purchasedValue,
                0D,
                (long) portfolioIds.size(),
                updatedAt,
                portfolioIds
            ));
        }
        return states;
    }

    private boolean hasPositiveAmount(Asset asset) {
        return asset.getStockId() != null
            && asset.getAmount() != null
            && asset.getAmount().compareTo(POSITIVE_AMOUNT_THRESHOLD) > 0
            && asset.getTotalPrice() != null
            && asset.getTotalPrice().getAmount() != null
            && asset.getTotalPrice().getCurrency() != null;
    }

    private long toLongAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private record WarmUpResult(long portfolioCount, long userCount, long holdingCount) {
    }
}
