package depth.finvibe.modules.asset.infra.persistence;

import java.time.Instant;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.asset.application.dto.PortfolioValuationSnapshot;
import depth.finvibe.modules.asset.application.port.out.PortfolioValuationRepository;
import depth.finvibe.modules.asset.domain.PortfolioValuationReadModel;

@Repository
@RequiredArgsConstructor
public class PortfolioValuationRepositoryImpl implements PortfolioValuationRepository {
    private final PortfolioValuationJpaRepository jpaRepository;

    @Override
    public void upsertIfNewer(PortfolioValuationSnapshot snapshot) {
        if (snapshot == null || snapshot.portfolioId() == null) {
            return;
        }

        PortfolioValuationReadModel valuation = jpaRepository.findById(snapshot.portfolioId())
            .orElseGet(() -> PortfolioValuationReadModel.create(
                snapshot.portfolioId(),
                snapshot.purchasedValue(),
                snapshot.currentValue(),
                snapshot.profitRate(),
                snapshot.assetCount(),
                snapshot.updatedAt()
            ));

        if (valuation.getPortfolioId() != null && jpaRepository.existsById(snapshot.portfolioId())) {
            valuation.updateIfNewer(
                snapshot.purchasedValue(),
                snapshot.currentValue(),
                snapshot.profitRate(),
                snapshot.assetCount(),
                snapshot.updatedAt()
            );
        }

        jpaRepository.save(valuation);
    }

    @Override
    public void softDeleteIfNewer(Long portfolioId, Instant deletedAt) {
        if (portfolioId == null || deletedAt == null) {
            return;
        }

        jpaRepository.findById(portfolioId)
            .ifPresent(valuation -> {
                valuation.softDeleteIfNewer(deletedAt);
                jpaRepository.save(valuation);
            });
    }

    @Override
    public List<Long> findActivePortfolioIdsAfter(Long lastPortfolioId, int limit) {
        return jpaRepository.findActivePortfolioIdsAfter(lastPortfolioId, PageRequest.of(0, limit));
    }

    @Override
    public List<PortfolioValuationReadModel> findActiveByPortfolioIds(List<Long> portfolioIds) {
        if (portfolioIds == null || portfolioIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findActiveByPortfolioIds(portfolioIds);
    }
}
