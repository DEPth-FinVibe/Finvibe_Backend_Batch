package depth.finvibe.modules.asset.infra.persistence;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import depth.finvibe.modules.asset.domain.PortfolioValuationReadModel;

public interface PortfolioValuationJpaRepository extends JpaRepository<PortfolioValuationReadModel, Long> {
    @Query("""
        select valuation.portfolioId
        from PortfolioValuationReadModel valuation
        where (:lastPortfolioId is null or valuation.portfolioId > :lastPortfolioId)
          and valuation.deleted = false
        order by valuation.portfolioId asc
        """)
    List<Long> findActivePortfolioIdsAfter(
        @Param("lastPortfolioId") Long lastPortfolioId,
        org.springframework.data.domain.Pageable pageable
    );

    @Query("""
        select valuation
        from PortfolioValuationReadModel valuation
        where valuation.portfolioId in :portfolioIds
          and valuation.deleted = false
        """)
    List<PortfolioValuationReadModel> findActiveByPortfolioIds(@Param("portfolioIds") Collection<Long> portfolioIds);
}
