package depth.finvibe.modules.asset.infra.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.asset.application.port.out.ValuationDirtyRepository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ValuationDirtyRedisRepository implements ValuationDirtyRepository {
    private static final String PORTFOLIO_VALUATION_DIRTY_KEY = "dirty:portfolio-valuations";
    private static final String USER_VALUATION_DIRTY_KEY = "dirty:user-valuations";
    private static final String PORTFOLIO_VALUATION_DELETION_DIRTY_KEY = "dirty:portfolio-valuation-deletions";

    private final StringRedisTemplate redisTemplate;

    @Override
    public void addPortfolioValuationDirty(List<Long> portfolioIds) {
        addAllToSet(
            PORTFOLIO_VALUATION_DIRTY_KEY,
            portfolioIds == null ? List.of() : portfolioIds.stream()
                .filter(java.util.Objects::nonNull)
                .map(String::valueOf)
                .toList()
        );
    }

    @Override
    public void addUserValuationDirty(List<String> userIds) {
        addAllToSet(
            USER_VALUATION_DIRTY_KEY,
            userIds == null ? List.of() : userIds.stream()
                .filter(userId -> userId != null && !userId.isBlank())
                .toList()
        );
    }

    @Override
    public List<Long> scanPortfolioValuationDirty(int limit) {
        return scanSet(PORTFOLIO_VALUATION_DIRTY_KEY, limit).stream()
            .map(this::parseLong)
            .flatMap(List::stream)
            .toList();
    }

    @Override
    public List<String> scanUserValuationDirty(int limit) {
        return scanSet(USER_VALUATION_DIRTY_KEY, limit);
    }

    @Override
    public List<Long> scanPortfolioValuationDeletionDirty(int limit) {
        return scanSet(PORTFOLIO_VALUATION_DELETION_DIRTY_KEY, limit).stream()
            .map(this::parseLong)
            .flatMap(List::stream)
            .toList();
    }

    @Override
    public void removePortfolioValuationDirty(Long portfolioId) {
        if (portfolioId != null) {
            redisTemplate.opsForSet().remove(PORTFOLIO_VALUATION_DIRTY_KEY, portfolioId.toString());
        }
    }

    @Override
    public void removeUserValuationDirty(String userId) {
        if (userId != null && !userId.isBlank()) {
            redisTemplate.opsForSet().remove(USER_VALUATION_DIRTY_KEY, userId);
        }
    }

    @Override
    public void removePortfolioValuationDeletionDirty(Long portfolioId) {
        if (portfolioId != null) {
            redisTemplate.opsForSet().remove(PORTFOLIO_VALUATION_DELETION_DIRTY_KEY, portfolioId.toString());
        }
    }

    @Override
    public long countPortfolioValuationDirty() {
        return setSize(PORTFOLIO_VALUATION_DIRTY_KEY);
    }

    @Override
    public long countUserValuationDirty() {
        return setSize(USER_VALUATION_DIRTY_KEY);
    }

    @Override
    public long countPortfolioValuationDeletionDirty() {
        return setSize(PORTFOLIO_VALUATION_DELETION_DIRTY_KEY);
    }

    private List<String> scanSet(String key, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        List<String> values = new ArrayList<>(limit);
        ScanOptions options = ScanOptions.scanOptions().count(limit).build();
        try (Cursor<String> cursor = redisTemplate.opsForSet().scan(key, options)) {
            while (cursor.hasNext() && values.size() < limit) {
                values.add(cursor.next());
            }
        }
        return values;
    }

    private void addAllToSet(String key, Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        redisTemplate.opsForSet().add(key, values.toArray(String[]::new));
    }

    private long setSize(String key) {
        Long size = redisTemplate.opsForSet().size(key);
        return size == null ? 0L : size;
    }

    private List<Long> parseLong(String value) {
        try {
            return List.of(Long.valueOf(value));
        } catch (NumberFormatException ex) {
            log.warn("Skip invalid dirty portfolio id: {}", value);
            return List.of();
        }
    }
}
