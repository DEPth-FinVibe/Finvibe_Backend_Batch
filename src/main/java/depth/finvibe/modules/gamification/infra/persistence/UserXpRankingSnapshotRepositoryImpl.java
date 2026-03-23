package depth.finvibe.modules.gamification.infra.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.gamification.application.port.out.UserXpRankingSnapshotRepository;
import depth.finvibe.modules.gamification.domain.UserXpRankingSnapshot;
import depth.finvibe.modules.gamification.domain.enums.RankingPeriod;

@Repository
public class UserXpRankingSnapshotRepositoryImpl implements UserXpRankingSnapshotRepository {
    private final UserXpRankingSnapshotJpaRepository userXpRankingSnapshotJpaRepository;
    private final EntityManager entityManager;

    public UserXpRankingSnapshotRepositoryImpl(
            UserXpRankingSnapshotJpaRepository userXpRankingSnapshotJpaRepository,
            EntityManager entityManager) {
        this.userXpRankingSnapshotJpaRepository = userXpRankingSnapshotJpaRepository;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional
    public void deleteSnapshots(RankingPeriod periodType, LocalDate periodStartDate) {
        userXpRankingSnapshotJpaRepository.deleteByPeriodTypeAndPeriodStartDate(periodType, periodStartDate);
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    @Transactional
    public void saveAll(List<UserXpRankingSnapshot> snapshots) {
        if (snapshots.isEmpty()) {
            return;
        }

        userXpRankingSnapshotJpaRepository.saveAll(snapshots);
        entityManager.flush();
        entityManager.clear();
    }

    @Override
    @Transactional
    public void replaceSnapshots(
            RankingPeriod periodType,
            LocalDate periodStartDate,
            List<UserXpRankingSnapshot> snapshots) {
        deleteSnapshots(periodType, periodStartDate);

        if (!snapshots.isEmpty()) {
            saveAll(snapshots);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserXpRankingSnapshot> findTopByPeriod(RankingPeriod periodType, LocalDate periodStartDate, int size) {
        int pageSize = Math.max(size, 1);
        return userXpRankingSnapshotJpaRepository.findByPeriodTypeAndPeriodStartDateOrderByRankingAsc(
                periodType,
                periodStartDate,
                PageRequest.of(0, pageSize));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserXpRankingSnapshot> findByPeriodAndUserId(
            RankingPeriod periodType,
            LocalDate periodStartDate,
            UUID userId) {
        return userXpRankingSnapshotJpaRepository.findByPeriodTypeAndPeriodStartDateAndUserId(
                periodType,
                periodStartDate,
                userId);
    }
}
