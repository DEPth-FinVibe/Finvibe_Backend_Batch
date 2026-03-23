package depth.finvibe.modules.gamification.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import depth.finvibe.modules.gamification.domain.UserMetric;
import depth.finvibe.modules.gamification.domain.enums.CollectPeriod;
import depth.finvibe.modules.gamification.domain.enums.UserMetricType;
import depth.finvibe.modules.gamification.domain.idclass.UserMetricId;

public interface MetricJpaRepository extends JpaRepository<UserMetric, UserMetricId> {
    List<UserMetric> findByTypeAndCollectPeriodAndValueGreaterThanEqual(
            UserMetricType type,
            CollectPeriod collectPeriod,
            Double value);

    List<UserMetric> findByTypeAndCollectPeriodOrderByValueDesc(
            UserMetricType type,
            CollectPeriod collectPeriod,
            Pageable pageable);

    Optional<UserMetric> findByUserIdAndTypeAndCollectPeriod(UUID userId, UserMetricType type, CollectPeriod collectPeriod);

    List<UserMetric> findByUserIdAndCollectPeriodAndTypeIn(
            UUID userId,
            CollectPeriod collectPeriod,
            List<UserMetricType> types);

    List<UserMetric> findByUserIdAndType(UUID userId, UserMetricType type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserMetric metric
            set metric.value = :value
            where metric.userId = :userId
              and metric.type = :type
              and metric.collectPeriod = :collectPeriod
            """)
    int updateValue(
            @Param("userId") UUID userId,
            @Param("type") UserMetricType type,
            @Param("collectPeriod") CollectPeriod collectPeriod,
            @Param("value") Double value);

    void deleteByCollectPeriod(CollectPeriod collectPeriod);
}
