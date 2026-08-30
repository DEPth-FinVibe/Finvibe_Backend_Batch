package depth.finvibe.modules.asset.application;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.dto.PortfolioValuationSnapshot;
import depth.finvibe.modules.asset.application.dto.UserValuationSnapshot;
import depth.finvibe.modules.asset.application.port.out.PortfolioValuationRepository;
import depth.finvibe.modules.asset.application.port.out.UserValuationRepository;

@Service
@RequiredArgsConstructor
public class ValuationWriteBackTransaction {
    private final PortfolioValuationRepository portfolioValuationRepository;
    private final UserValuationRepository userValuationRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deletePortfolios(List<PortfolioDeletion> deletions) {
        deletions.forEach(deletion ->
            portfolioValuationRepository.softDeleteIfNewer(deletion.portfolioId(), deletion.deletedAt()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertPortfolios(List<PortfolioValuationSnapshot> snapshots) {
        snapshots.forEach(portfolioValuationRepository::upsertIfNewer);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void upsertUsers(List<UserValuationSnapshot> snapshots) {
        snapshots.forEach(userValuationRepository::upsertIfNewer);
    }

    public record PortfolioDeletion(Long portfolioId, Instant deletedAt) {
    }
}
