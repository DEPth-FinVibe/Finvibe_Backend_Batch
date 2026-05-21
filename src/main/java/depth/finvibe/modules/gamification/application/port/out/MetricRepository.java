package depth.finvibe.modules.gamification.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import depth.finvibe.modules.gamification.domain.UserMetric;
import depth.finvibe.modules.gamification.domain.enums.CollectPeriod;
import depth.finvibe.modules.gamification.domain.enums.UserMetricType;

public interface MetricRepository {
    List<Long> findUsersAchieved(UserMetricType metricType, CollectPeriod collectPeriod, Double targetValue);

    List<Long> findTopUsersByMetric(UserMetricType metricType, CollectPeriod collectPeriod, int limit);

    Optional<UserMetric> findByUserIdAndType(Long userId, UserMetricType type, CollectPeriod collectPeriod);

    Map<UserMetricType, UserMetric> findByUserIdAndCollectPeriodAndTypes(
            Long userId,
            CollectPeriod collectPeriod,
            List<UserMetricType> metricTypes);

    Map<CollectPeriod, UserMetric> findByUserIdAndTypeAcrossPeriods(Long userId, UserMetricType metricType);

    UserMetric save(UserMetric userMetric);

    void upsertValue(Long userId, UserMetricType metricType, CollectPeriod collectPeriod, double value);

    void deleteAllByCollectPeriod(CollectPeriod collectPeriod);
}
