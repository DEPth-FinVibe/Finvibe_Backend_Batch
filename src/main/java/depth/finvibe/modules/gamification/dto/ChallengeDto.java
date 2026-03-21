package depth.finvibe.modules.gamification.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import depth.finvibe.modules.gamification.domain.PersonalChallenge;
import depth.finvibe.modules.gamification.domain.enums.UserMetricType;

public class ChallengeDto {

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ChallengeGenerationResponse {
        private String title;
        private String description;
        private UserMetricType metricType;
        private Double targetValue;
        private Long rewardXp;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ChallengeResponse {
        private Long id;
        private String title;
        private String description;
        private UserMetricType metricType;
        private Double targetValue;
        private Double currentValue;
        private Double progressPercentage;
        private Long rewardXp;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean isAchieved;

        public static ChallengeResponse from(PersonalChallenge challenge, Double currentValue) {
            Double targetValue = challenge.getCondition().getTargetValue();
            return ChallengeResponse.builder()
                    .id(challenge.getId())
                    .title(challenge.getTitle())
                    .description(challenge.getDescription())
                    .metricType(challenge.getCondition().getMetricType())
                    .targetValue(targetValue)
                    .currentValue(currentValue)
                    .progressPercentage(Math.min(100.0, (currentValue / targetValue) * 100.0))
                    .rewardXp(challenge.getReward().getRewardXp())
                    .startDate(challenge.getPeriod().getStartDate())
                    .endDate(challenge.getPeriod().getEndDate())
                    .isAchieved(currentValue >= targetValue)
                    .build();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ChallengeHistoryResponse {
        private Long challengeId;
        private String title;
        private String description;
        private UserMetricType metricType;
        private Double targetValue;
        private Long rewardXp;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDate completedAt;

        public static ChallengeHistoryResponse from(PersonalChallenge challenge, LocalDate completedAt) {
            return ChallengeHistoryResponse.builder()
                    .challengeId(challenge.getId())
                    .title(challenge.getTitle())
                    .description(challenge.getDescription())
                    .metricType(challenge.getCondition().getMetricType())
                    .targetValue(challenge.getCondition().getTargetValue())
                    .rewardXp(challenge.getReward().getRewardXp())
                    .startDate(challenge.getPeriod().getStartDate())
                    .endDate(challenge.getPeriod().getEndDate())
                    .completedAt(completedAt)
                    .build();
        }
    }
}
