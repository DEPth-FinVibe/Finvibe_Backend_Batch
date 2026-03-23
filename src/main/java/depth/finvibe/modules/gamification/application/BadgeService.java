package depth.finvibe.modules.gamification.application;

import depth.finvibe.modules.gamification.application.port.in.BadgeCommandUseCase;
import depth.finvibe.modules.gamification.application.port.out.BadgeOwnershipRepository;
import depth.finvibe.modules.gamification.domain.BadgeOwnership;
import depth.finvibe.modules.gamification.domain.enums.Badge;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BadgeService implements BadgeCommandUseCase {

    private final BadgeOwnershipRepository badgeOwnershipRepository;

    @Override
    @Transactional
    public void grantBadgeToUser(UUID userId, Badge badge) {
        BadgeOwnership toSave = BadgeOwnership.of(badge, userId);

        if (badgeOwnershipRepository.isExist(toSave)) {
            return;
        }

        badgeOwnershipRepository.save(toSave);
    }
}
