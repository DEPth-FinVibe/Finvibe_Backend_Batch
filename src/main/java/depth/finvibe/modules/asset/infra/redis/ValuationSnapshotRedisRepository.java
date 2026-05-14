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

        List<String> hashValues = hashMultiGet(portfolioHashKey(portfolioId), List.of("pv", "cv", "pr", "ac", "ua"));
        if (hashValues != null && hashValues.size() >= 5 && !hasMissingRequired(hashValues, 4)) {
            return Optional.of(new PortfolioValuationSnapshot(
                portfolioId,
                parseLong(hashValues.get(0), portfolioId, "pv"),
                parseLong(hashValues.get(1), portfolioId, "cv"),
                parseDouble(hashValues.get(2), portfolioId, "pr"),
                parseLong(hashValues.get(3), portfolioId, "ac"),
                parseInstant(hashValues.get(4), portfolioId, "ua").orElse(null)
            ));
        }
        log.warn("Missing portfolio valuation snapshot. portfolioId={}", portfolioId);
        return Optional.empty();
    }

    @Override
    public Optional<UserValuationSnapshot> readUserSnapshot(String userId) {
        if (userId == null || userId.isBlank()) {
            return Optional.empty();
        }

        List<String> hashValues = hashMultiGet(userHashKey(userId), List.of("pv", "cv", "pr", "pc", "ua"));
        if (hashValues != null && hashValues.size() >= 5 && !hasMissingRequired(hashValues, 4)) {
            return Optional.of(new UserValuationSnapshot(
                userId,
                parseLong(hashValues.get(0), userId, "pv"),
                parseLong(hashValues.get(1), userId, "cv"),
                parseDouble(hashValues.get(2), userId, "pr"),
                parseLong(hashValues.get(3), userId, "pc"),
                parseInstant(hashValues.get(4), userId, "ua").orElse(null)
            ));
        }
        log.warn("Missing user valuation snapshot. userId={}", userId);
        return Optional.empty();
    }

    @Override
    public boolean isPortfolioDeleted(Long portfolioId) {
        Object hashValue = redisTemplate.opsForHash().get(portfolioHashKey(portfolioId), "del");
        if (hashValue != null) {
            return parseBoolean(hashValue.toString());
        }
        return false;
    }

    @Override
    public Optional<Instant> readPortfolioDeletedAt(Long portfolioId) {
        Object hashValue = redisTemplate.opsForHash().get(portfolioHashKey(portfolioId), "da");
        if (hashValue != null) {
            return parseInstant(hashValue.toString(), portfolioId, "da");
        }
        return Optional.empty();
    }

    private List<String> hashMultiGet(String key, List<String> fields) {
        List<Object> values = redisTemplate.opsForHash().multiGet(key, fields.stream()
            .map(field -> (Object) field)
            .toList());
        if (values == null) {
            return null;
        }
        return values.stream()
            .map(value -> value == null ? null : value.toString())
            .toList();
    }

    private boolean hasMissingRequired(List<String> values, int requiredCount) {
        for (int i = 0; i < requiredCount; i++) {
            if (values.get(i) == null || values.get(i).isBlank()) {
                return true;
            }
        }
        return false;
    }

    private String portfolioHashKey(Long portfolioId) {
        return "pf:" + portfolioId;
    }

    private String userHashKey(String userId) {
        return "usr:" + userId;
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

    private boolean parseBoolean(String value) {
        return "1".equals(value) || Boolean.parseBoolean(value);
    }
}
