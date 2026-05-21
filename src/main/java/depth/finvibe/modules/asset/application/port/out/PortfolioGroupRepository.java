package depth.finvibe.modules.asset.application.port.out;

import java.util.List;
import java.util.Optional;

import depth.finvibe.modules.asset.domain.PortfolioGroup;

public interface PortfolioGroupRepository {
    PortfolioGroup save(PortfolioGroup portfolioGroup);

    List<PortfolioGroup> saveAll(List<PortfolioGroup> portfolioGroups);

    Optional<PortfolioGroup> findById(Long id);

    List<PortfolioGroup> findAllWithAssets();

    List<Long> findPortfolioIdsAfter(Long lastPortfolioId, int limit);

    List<PortfolioGroup> findAllWithAssetsByIds(List<Long> portfolioIds);

    List<Long> findUserIdsAfter(Long lastUserId, int limit);

    List<PortfolioGroup> findAllWithAssetsByUserIds(List<Long> userIds);

    void delete(PortfolioGroup existing);
}
