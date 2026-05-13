package depth.finvibe.modules.asset.application.port.out;

import java.time.Instant;
import java.util.List;

import depth.finvibe.modules.asset.application.dto.PortfolioValuationSnapshot;
import depth.finvibe.modules.asset.domain.PortfolioValuationReadModel;

public interface PortfolioValuationRepository {
    void upsertIfNewer(PortfolioValuationSnapshot snapshot);

    void softDeleteIfNewer(Long portfolioId, Instant deletedAt);

    List<Long> findActivePortfolioIdsAfter(Long lastPortfolioId, int limit);

    List<PortfolioValuationReadModel> findActiveByPortfolioIds(List<Long> portfolioIds);
}
