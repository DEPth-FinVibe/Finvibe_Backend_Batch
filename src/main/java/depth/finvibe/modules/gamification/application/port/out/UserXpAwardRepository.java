package depth.finvibe.modules.gamification.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import depth.finvibe.modules.gamification.domain.UserXpAward;

public interface UserXpAwardRepository {
    record UserPeriodXp(Long userId, Long xp) {
    }

    void save(UserXpAward userXpAward);
    List<UserXpAward> findByUserId(Long userId);

    /**
     * XP 총합 기준 상위 N명의 사용자 ID를 조회
     *
     * @param limit 상위 N명
     * @return 상위 N명의 사용자 ID 목록 (XP 총합 내림차순)
     */
    List<Long> findTopUsersByTotalXp(int limit);

    List<UserPeriodXp> findUserPeriodXpRankingBetween(
            LocalDateTime startInclusive,
            LocalDateTime endExclusive,
            Long lastXp,
            Long lastUserId,
            int limit);

    Map<Long, Long> findUserPeriodXpMapBetween(
            List<Long> userIds,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive);
}
