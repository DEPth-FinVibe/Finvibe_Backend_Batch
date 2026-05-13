package depth.finvibe.modules.asset.infra.persistence;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import depth.finvibe.modules.asset.domain.UserValuation;

public interface UserValuationJpaRepository extends JpaRepository<UserValuation, String> {
    @Query("""
        select valuation
        from UserValuation valuation
        where (:lastUserId is null or valuation.userId > :lastUserId)
        order by valuation.userId asc
        """)
    List<UserValuation> findAfter(@Param("lastUserId") String lastUserId, Pageable pageable);
}
