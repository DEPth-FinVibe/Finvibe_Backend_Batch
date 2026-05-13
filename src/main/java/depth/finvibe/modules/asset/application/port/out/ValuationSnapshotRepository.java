package depth.finvibe.modules.asset.application.port.out;

import java.time.Instant;
import java.util.Optional;

import depth.finvibe.modules.asset.application.dto.PortfolioValuationSnapshot;
import depth.finvibe.modules.asset.application.dto.UserValuationSnapshot;

public interface ValuationSnapshotRepository {
    Optional<PortfolioValuationSnapshot> readPortfolioSnapshot(Long portfolioId);

    Optional<UserValuationSnapshot> readUserSnapshot(String userId);

    boolean isPortfolioDeleted(Long portfolioId);

    Optional<Instant> readPortfolioDeletedAt(Long portfolioId);
}
