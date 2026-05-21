package depth.finvibe.modules.asset.infra.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.asset.domain.Currency;
import depth.finvibe.modules.asset.domain.PortfolioGroup;

import static depth.finvibe.modules.asset.domain.QAsset.asset;
import static depth.finvibe.modules.asset.domain.QPortfolioGroup.portfolioGroup;

@Repository
@RequiredArgsConstructor
public class PortfolioGroupQueryRepository {
    private final JPAQueryFactory queryFactory;

    public List<PortfolioGroup> findAllWithAssets() {
        return queryFactory
                .selectFrom(portfolioGroup)
                .leftJoin(portfolioGroup.assets, asset).fetchJoin()
                .distinct()
                .fetch();
    }

    public List<Long> findPortfolioIdsAfter(Long lastPortfolioId, int limit) {
        return queryFactory
                .select(portfolioGroup.id)
                .from(portfolioGroup)
                .where(lastPortfolioId == null ? null : portfolioGroup.id.gt(lastPortfolioId))
                .orderBy(portfolioGroup.id.asc())
                .limit(limit)
                .fetch();
    }

    public List<PortfolioGroup> findAllWithAssetsByIds(List<Long> portfolioIds) {
        if (portfolioIds == null || portfolioIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(portfolioGroup)
                .leftJoin(portfolioGroup.assets, asset).fetchJoin()
                .where(portfolioGroup.id.in(portfolioIds))
                .orderBy(portfolioGroup.id.asc())
                .distinct()
                .fetch();
    }

    public List<PortfolioAssetRow> findPortfolioAssetRowsByIds(List<Long> portfolioIds) {
        if (portfolioIds == null || portfolioIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(Projections.constructor(
                        PortfolioAssetRow.class,
                        portfolioGroup.id,
                        portfolioGroup.userId,
                        asset.stockId,
                        asset.amount,
                        asset.totalPrice.amount,
                        asset.totalPrice.currency
                ))
                .from(portfolioGroup)
                .leftJoin(portfolioGroup.assets, asset)
                .where(portfolioGroup.id.in(portfolioIds))
                .orderBy(portfolioGroup.id.asc(), asset.stockId.asc())
                .fetch();
    }

    public List<Long> findUserIdsAfter(Long lastUserId, int limit) {
        return queryFactory
                .select(portfolioGroup.userId)
                .distinct()
                .from(portfolioGroup)
                .where(lastUserId == null ? null : portfolioGroup.userId.gt(lastUserId))
                .orderBy(portfolioGroup.userId.asc())
                .limit(limit)
                .fetch();
    }

    public List<PortfolioGroup> findAllWithAssetsByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .selectFrom(portfolioGroup)
                .leftJoin(portfolioGroup.assets, asset).fetchJoin()
                .where(portfolioGroup.userId.in(userIds))
                .orderBy(portfolioGroup.userId.asc(), portfolioGroup.id.asc())
                .distinct()
                .fetch();
    }

    public record PortfolioAssetRow(
            Long portfolioId,
            UUID userId,
            Long stockId,
            BigDecimal amount,
            BigDecimal purchasePriceAmount,
            Currency currency
    ) {
    }
}
