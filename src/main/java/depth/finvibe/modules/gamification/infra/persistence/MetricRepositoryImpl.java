package depth.finvibe.modules.gamification.infra.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.gamification.application.port.out.MetricRepository;
import depth.finvibe.modules.gamification.domain.UserMetric;
import depth.finvibe.modules.gamification.domain.enums.CollectPeriod;
import depth.finvibe.modules.gamification.domain.enums.UserMetricType;

@Repository
@RequiredArgsConstructor
public class MetricRepositoryImpl implements MetricRepository {
    private final MetricJpaRepository metricJpaRepository;

    @Override
    public List<UUID> findUsersAchieved(UserMetricType metricType, CollectPeriod collectPeriod, Double targetValue) {
        return metricJpaRepository.findByTypeAndCollectPeriodAndValueGreaterThanEqual(metricType, collectPeriod, targetValue).stream()
                .map(UserMetric::getUserId)
                .toList();
    }

    @Override
    public List<UUID> findTopUsersByMetric(UserMetricType metricType, CollectPeriod collectPeriod, int limit) {
        return metricJpaRepository.findByTypeAndCollectPeriodOrderByValueDesc(
                        metricType,
                        collectPeriod,
                        PageRequest.of(0, limit))
                .stream()
                .map(UserMetric::getUserId)
                .toList();
    }

    @Override
    public Optional<UserMetric> findByUserIdAndType(UUID userId, UserMetricType type, CollectPeriod collectPeriod) {
        return metricJpaRepository.findByUserIdAndTypeAndCollectPeriod(userId, type, collectPeriod);
    }

    @Override
    public Map<UserMetricType, UserMetric> findByUserIdAndCollectPeriodAndTypes(
            UUID userId,
            CollectPeriod collectPeriod,
            List<UserMetricType> metricTypes) {
        if (metricTypes == null || metricTypes.isEmpty()) {
            return Map.of();
        }

        return metricJpaRepository.findByUserIdAndCollectPeriodAndTypeIn(userId, collectPeriod, metricTypes).stream()
                .collect(Collectors.toMap(UserMetric::getType, Function.identity()));
    }

    @Override
    public Map<CollectPeriod, UserMetric> findByUserIdAndTypeAcrossPeriods(UUID userId, UserMetricType metricType) {
        return metricJpaRepository.findByUserIdAndType(userId, metricType).stream()
                .collect(Collectors.toMap(UserMetric::getCollectPeriod, Function.identity()));
    }

    @Override
    public UserMetric save(UserMetric userMetric) {
        return metricJpaRepository.save(userMetric);
    }

    @Override
    public void deleteAllByCollectPeriod(CollectPeriod collectPeriod) {
        metricJpaRepository.deleteByCollectPeriod(collectPeriod);
    }
}
