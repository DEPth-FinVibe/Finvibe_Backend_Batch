package depth.finvibe.modules.user.application.service;

import depth.finvibe.common.error.DomainException;
import depth.finvibe.modules.user.application.port.out.UserIdCacheRepository;
import depth.finvibe.modules.user.application.port.out.UserRepository;
import depth.finvibe.modules.user.domain.User;
import depth.finvibe.modules.user.domain.error.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserIdResolver {

    private final UserIdCacheRepository userIdCacheRepository;
    private final UserRepository userRepository;

    public Long resolveInternalUserId(UUID externalUserId) {
        return resolveInternalUserIdIfPresent(externalUserId)
            .orElseThrow(() -> new DomainException(UserErrorCode.USER_NOT_FOUND));
    }

    public Optional<Long> resolveInternalUserIdIfPresent(UUID externalUserId) {
        if (externalUserId == null) {
            return Optional.empty();
        }
        return userIdCacheRepository.findInternalUserIdByExternalUserId(externalUserId)
            .or(() -> loadAndCacheInternalUserIdIfPresent(externalUserId));
    }

    private Optional<Long> loadAndCacheInternalUserIdIfPresent(UUID externalUserId) {
        Optional<User> foundUser = userRepository.findByExternalUserId(externalUserId);
        if (foundUser.isEmpty()) {
            return Optional.empty();
        }
        User user = foundUser.get();
        Long internalUserId = user.getInternalUserId();
        if (internalUserId == null) {
            return Optional.empty();
        }
        userIdCacheRepository.save(externalUserId, internalUserId);
        return Optional.of(internalUserId);
    }
}
