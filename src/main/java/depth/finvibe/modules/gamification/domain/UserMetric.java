package depth.finvibe.modules.gamification.domain;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import depth.finvibe.modules.gamification.domain.enums.UserMetricType;
import depth.finvibe.modules.gamification.domain.enums.CollectPeriod;
import depth.finvibe.modules.gamification.domain.idclass.UserMetricId;
import depth.finvibe.common.gamification.domain.TimeStampedBaseEntity;

@Entity
@Table(indexes = {
    @Index(name = "idx_user_metric_user_collect_type", columnList = "user_id, collect_period, type"),
    @Index(name = "idx_user_metric_type_collect_value", columnList = "type, collect_period, value")
})
@IdClass(UserMetricId.class)
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@SuperBuilder
public class UserMetric extends TimeStampedBaseEntity {
    @Id
    @Enumerated(EnumType.STRING)
    private UserMetricType type;

    @Id
    private UUID userId;

    @Id
    @Enumerated(EnumType.STRING)
    private CollectPeriod collectPeriod;

    private Double value;
}
