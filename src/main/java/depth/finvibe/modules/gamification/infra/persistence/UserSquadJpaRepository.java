package depth.finvibe.modules.gamification.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import depth.finvibe.modules.gamification.domain.UserSquad;

public interface UserSquadJpaRepository extends JpaRepository<UserSquad, UUID> {
    Optional<UserSquad> findByUserId(UUID userId);

    @Query("select userSquad.squad.id from UserSquad userSquad where userSquad.userId = :userId")
    Optional<Long> findSquadIdByUserId(@Param("userId") UUID userId);

    List<UserSquad> findAllBySquadId(Long squadId);
}
