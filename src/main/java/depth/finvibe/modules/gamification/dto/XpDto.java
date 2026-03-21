package depth.finvibe.modules.gamification.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import depth.finvibe.modules.gamification.domain.UserXp;

public class XpDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GrantXpRequest {
        private Long value;
        private String reason;
    }

    @Getter
    @Builder
    public static class Response {
        private UUID userId;
        private String nickname;
        private Long totalXp;
        private Integer level;

        public static Response from(UserXp userXp) {
            return Response.builder()
                    .userId(userXp.getUserId())
                    .nickname(userXp.getNickname())
                    .totalXp(userXp.getTotalXp())
                    .level(userXp.getLevel())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SquadRankingResponse {
        private Long squadId;
        private String squadName;
        private Integer currentRanking;
        private Long totalXp;
        private Long weeklyXp;
        private Double weeklyXpChangeRate;
        private Integer rankingChange; // +2, -1, 0 등
    }

    @Getter
    @Builder
    public static class ContributionRankingResponse {
        private String nickname;
        private Integer ranking;
        private Long weeklyContributionXp;
    }

    @Getter
    @Builder
    public static class UserRankingResponse {
        private UUID userId;
        private String nickname;
        private Integer ranking;
        private Long currentXp;
        private Long periodXp;
        private Long previousPeriodXp;
        private Double growthRate;
    }
}
