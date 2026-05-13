package depth.finvibe.modules.asset.infra.redis;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.asset.application.dto.PortfolioValuationSnapshot;
import depth.finvibe.modules.asset.application.dto.UserValuationSnapshot;
import depth.finvibe.modules.asset.application.port.out.ValuationSnapshotRepository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ValuationSnapshotRedisRepository implements ValuationSnapshotRepository {
    private final StringRedisTemplate redisTemplate;

    @Override
    public Optional<PortfolioValuationSnapshot> readPortfolioSnapshot(Long portfolioId) {
        if (portfolioId == null) {
            return Optional.empty();
        }

        List<String> values = redisTemplate.opsForValue().multiGet(List.of(
            portfolioKey(portfolioId, "purchased-value"),
            portfolioKey(portfolioId, "current-value"),
            portfolioKey(portfolioId, "profit-rate"),
            portfolioKey(portfolioId, "asset-count"),
            portfolioKey(portfolioId, "updated-at")
        ));
        if (values == null || values.size() < 5 || hasMissingRequired(values, 4)) {
            log.warn("Missing portfolio valuation snapshot. portfolioId={}", portfolioId);
            return Optional.empty();
        }

        return Optional.of(new PortfolioValuationSnapshot(
            portfolioId,
            parseLong(values.get(0), portfolioId, "purchased-value"),
            parseLong(values.get(1), portfolioId, "current-value"),
            parseDouble(values.get(2), portfolioId, "profit-rate"),
            parseLong(values.get(3), portfolioId, "asset-count"),
            parseInstant(values.get(4), portfolioId, "updated-at").orElse(null)
        ));
    }

    @Override
    public Optional<UserValuationSnapshot> readUserSnapshot(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        List<String> values = redisTemplate.opsForValue().multiGet(List.of(
            userKey(userId, "purchased-value"),
            userKey(userId, "current-value"),
            userKey(userId, "profit-rate"),
            userKey(userId, "portfolio-count"),
            userKey(userId, "updated-at")
        ));
        if (values == null || values.size() < 5 || hasMissingRequired(values, 4)) {
            log.warn("Missing user valuation snapshot. userId={}", userId);
            return Optional.empty();
        }

        return Optional.of(new UserValuationSnapshot(
            userId,
            parseLong(values.get(0), userId, "purchased-value"),
            parseLong(values.get(1), userId, "current-value"),
            parseDouble(values.get(2), userId, "profit-rate"),
            parseLong(values.get(3), userId, "portfolio-count"),
            parseInstant(values.get(4), userId, "updated-at").orElse(null)
        ));
    }

    @Override
    public boolean isPortfolioDeleted(Long portfolioId) {
        String value = redisTemplate.opsForValue().get(portfolioKey(portfolioId, "deleted"));
        return Boolean.parseBoolean(value);
    }

    @Override
    public Optional<Instant> readPortfolioDeletedAt(Long portfolioId) {
        String value = redisTemplate.opsForValue().get(portfolioKey(portfolioId, "deleted-at"));
        return parseInstant(value, portfolioId, "deleted-at");
    }

    private boolean hasMissingRequired(List<String> values, int requiredCount) {
        for (int i = 0; i < requiredCount; i++) {
            if (values.get(i) == null || values.get(i).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String portfolioKey(Long portfolioId, String field) {
        return "portfolio:" + portfolioId + ":" + field;
    }

    private String userKey(String userId, String field) {
        return "user:" + userId + ":" + field;
    }

    private Long parseLong(String value, Object id, String field) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid valuation long field. id=" + id + ", field=" + field, ex);
        }
    }

    private Double parseDouble(String value, Object id, String field) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid valuation double field. id=" + id + ", field=" + field, ex);
        }
    }

    private Optional<Instant> parseInstant(String value, Object id, String field) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Instant.parse(value));
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("Invalid valuation instant field. id=" + id + ", field=" + field, ex);
        }
    }
}
