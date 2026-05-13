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
    name = "portfolio_valuation",
    indexes = {
        @Index(name = "idx_portfolio_valuation_deleted", columnList = "deleted"),
        @Index(name = "idx_portfolio_valuation_updated_at", columnList = "updated_at")
    }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PortfolioValuationReadModel {
    @Id
    @Column(name = "portfolio_id", nullable = false)
    private Long portfolioId;

    @Column(name = "purchased_value", nullable = false)
    private Long purchasedValue;

    @Column(name = "current_value", nullable = false)
    private Long currentValue;

    @Column(name = "profit_rate", nullable = false)
    private Double profitRate;

    @Column(name = "asset_count", nullable = false)
    private Long assetCount;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public static PortfolioValuationReadModel create(
        Long portfolioId,
        Long purchasedValue,
        Long currentValue,
        Double profitRate,
        Long assetCount,
        Instant updatedAt
    ) {
        return PortfolioValuationReadModel.builder()
            .portfolioId(portfolioId)
            .purchasedValue(purchasedValue)
            .currentValue(currentValue)
            .profitRate(profitRate)
            .assetCount(assetCount)
            .updatedAt(updatedAt)
            .deleted(false)
            .build();
    }

    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }

    public void updateIfNewer(Long purchasedValue, Long currentValue, Double profitRate, Long assetCount, Instant updatedAt) {
        if (isDeleted() || !isNewerThanCurrent(updatedAt)) {
            return;
        }

        this.purchasedValue = purchasedValue;
        this.currentValue = currentValue;
        this.profitRate = profitRate;
        this.assetCount = assetCount;
        this.updatedAt = updatedAt;
    }

    public void softDeleteIfNewer(Instant deletedAt) {
        if (deletedAt == null) {
            return;
        }
        if (this.deletedAt != null && !deletedAt.isAfter(this.deletedAt)) {
            return;
        }

        this.deleted = true;
        this.deletedAt = deletedAt;
    }

    private boolean isNewerThanCurrent(Instant newUpdatedAt) {
        return this.updatedAt == null || newUpdatedAt == null || newUpdatedAt.isAfter(this.updatedAt);
    }
}
