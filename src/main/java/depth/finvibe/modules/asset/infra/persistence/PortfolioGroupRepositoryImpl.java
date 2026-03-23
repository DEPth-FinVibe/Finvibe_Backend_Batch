package depth.finvibe.modules.asset.infra.persistence;

import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.modules.asset.domain.PortfolioGroup;

@Repository
@RequiredArgsConstructor
public class PortfolioGroupRepositoryImpl implements PortfolioGroupRepository {
    private final PortfolioGroupJpaRepository jpaRepository;
    private final PortfolioGroupQueryRepository queryRepository;

    @Override
    public PortfolioGroup save(PortfolioGroup portfolioGroup) {
        return jpaRepository.save(portfolioGroup);
    }

    @Override
    public List<PortfolioGroup> saveAll(List<PortfolioGroup> portfolioGroups) {
        return jpaRepository.saveAll(portfolioGroups);
    }

    @Override
    public Optional<PortfolioGroup> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<PortfolioGroup> findAllWithAssets() {
        return queryRepository.findAllWithAssets();
    }

    @Override
    public List<Long> findPortfolioIdsAfter(Long lastPortfolioId, int limit) {
        return queryRepository.findPortfolioIdsAfter(lastPortfolioId, limit);
    }

    @Override
    public List<PortfolioGroup> findAllWithAssetsByIds(List<Long> portfolioIds) {
        return queryRepository.findAllWithAssetsByIds(portfolioIds);
    }

    @Override
    public void delete(PortfolioGroup existing) {
        jpaRepository.delete(existing);
    }
}
