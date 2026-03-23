package depth.finvibe.modules.asset.infra.persistence;

import java.util.List;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
