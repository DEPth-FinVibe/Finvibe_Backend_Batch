package depth.finvibe.modules.gamification.domain;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import depth.finvibe.modules.gamification.domain.vo.Xp;
import depth.finvibe.common.gamification.domain.TimeStampedBaseEntity;

@Entity
@Table(indexes = {
    @Index(name = "idx_user_xp_award_created_user", columnList = "created_at, user_id"),
    @Index(name = "idx_user_xp_award_user_created", columnList = "user_id, created_at")
})
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Getter
public class UserXpAward extends TimeStampedBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID userId;

    @Embedded
    private Xp xp;

    public static UserXpAward of(UUID userId, Xp xp) {
        return UserXpAward.builder()
                .userId(userId)
                .xp(xp)
                .build();
    }
}
