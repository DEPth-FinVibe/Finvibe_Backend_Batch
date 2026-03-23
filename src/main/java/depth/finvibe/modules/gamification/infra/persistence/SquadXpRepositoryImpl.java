package depth.finvibe.modules.gamification.infra.persistence;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import depth.finvibe.modules.gamification.application.port.out.SquadXpRepository;
import depth.finvibe.modules.gamification.domain.Squad;
import depth.finvibe.modules.gamification.domain.SquadXp;

@Repository
@RequiredArgsConstructor
public class SquadXpRepositoryImpl implements SquadXpRepository {
    private final SquadXpJpaRepository squadXpJpaRepository;
    private final EntityManager entityManager;

    @Override
    public void save(SquadXp squadXp) {
        squadXpJpaRepository.save(squadXp);
    }

    @Override
    public void saveAll(List<SquadXp> squadXps) {
        squadXpJpaRepository.saveAll(squadXps);
    }

    @Override
    public Optional<SquadXp> findBySquadId(Long squadId) {
        return squadXpJpaRepository.findBySquadId(squadId);
    }

    @Override
    public void addXp(Long squadId, Long amount) {
        int updatedRowCount = squadXpJpaRepository.addXp(squadId, amount);
        if (updatedRowCount > 0) {
            return;
        }

        Squad squadReference = entityManager.getReference(Squad.class, squadId);
        squadXpJpaRepository.save(SquadXp.builder()
                .squad(squadReference)
                .totalXp(amount)
                .weeklyXp(amount)
                .weeklyXpChangeRate(100.0)
                .build());
    }

    @Override
    public List<SquadXp> findAllByOrderByTotalXpDesc() {
        return squadXpJpaRepository.findAllByOrderByTotalXpDesc();
    }
}
