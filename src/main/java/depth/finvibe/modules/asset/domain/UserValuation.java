package depth.finvibe.modules.asset.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "user_valuation",
    indexes = {
        @Index(name = "idx_user_valuation_updated_at", columnList = "updated_at")
    }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UserValuation {
    @Id
    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "purchased_value", nullable = false)
    private Long purchasedValue;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;

    @Column(name = "profit_rate", nullable = false)
    private Double profitRate;

    @Column(name = "portfolio_count", nullable = false)
    private Long portfolioCount;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public static UserValuation create(
        String userId,
        Long purchasedValue,
        Long currentValue,
        Double profitRate,
        Long portfolioCount,
        Instant updatedAt
    ) {
        return UserValuation.builder()
            .userId(userId)
            .purchasedValue(purchasedValue)
            .currentValue(currentValue)
            .profitRate(profitRate)
            .portfolioCount(portfolioCount)
            .updatedAt(updatedAt)
            .build();
    }

    public void updateIfNewer(Long purchasedValue, Long currentValue, Double profitRate, Long portfolioCount, Instant updatedAt) {
        if (!isNewerThanCurrent(updatedAt)) {
            return;
        }

        this.purchasedValue = purchasedValue;
        this.currentValue = currentValue;
        this.profitRate = profitRate;
        this.portfolioCount = portfolioCount;
        this.updatedAt = updatedAt;
    }

    private boolean isNewerThanCurrent(Instant newUpdatedAt) {
        return newUpdatedAt != null && (this.updatedAt == null || newUpdatedAt.isAfter(this.updatedAt));
    }
}
