package depth.finvibe.modules.gamification.infra.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import depth.finvibe.modules.gamification.domain.UserXp;

public interface UserXpJpaRepository extends JpaRepository<UserXp, UUID> {
    Optional<UserXp> findByUserId(UUID userId);

    List<UserXp> findAllByUserIdInOrderByWeeklyXpDesc(List<UUID> userIds);

    List<UserXp> findAllByUserIdIn(List<UUID> userIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserXp userXp set userXp.weeklyXp = 0")
    int resetAllWeeklyXp();
}
