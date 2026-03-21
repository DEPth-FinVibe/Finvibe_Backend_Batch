package depth.finvibe.modules.asset.infra.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import depth.finvibe.modules.asset.domain.PortfolioGroup;

public interface PortfolioGroupJpaRepository extends JpaRepository<PortfolioGroup, Long> {}
