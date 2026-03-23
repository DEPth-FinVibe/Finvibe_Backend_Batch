package depth.finvibe.modules.gamification.infra.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import depth.finvibe.modules.gamification.domain.SquadXp;

public interface SquadXpJpaRepository extends JpaRepository<SquadXp, Long> {
    Optional<SquadXp> findBySquadId(Long squadId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SquadXp squadXp
            set squadXp.totalXp = squadXp.totalXp + :amount,
                squadXp.weeklyXp = squadXp.weeklyXp + :amount,
                squadXp.weeklyXpChangeRate =
                    case
                        when squadXp.totalXp <= 0 then 100.0
                        else ((squadXp.weeklyXp + :amount) * 100.0) / squadXp.totalXp
                    end
            where squadXp.squadId = :squadId
            """)
    int addXp(@Param("squadId") Long squadId, @Param("amount") Long amount);

    List<SquadXp> findAllByOrderByTotalXpDesc();
}
