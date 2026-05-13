package depth.finvibe.modules.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import depth.finvibe.common.error.DomainException;
import depth.finvibe.modules.user.application.port.out.UserIdCacheRepository;
import depth.finvibe.modules.user.application.port.out.UserRepository;
import depth.finvibe.modules.user.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserIdResolverTest {

    @Mock
    private UserIdCacheRepository userIdCacheRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    void returnsInternalUserIdFromCache() {
        UUID externalUserId = UUID.randomUUID();
        UserIdResolver resolver = new UserIdResolver(userIdCacheRepository, userRepository);

        when(userIdCacheRepository.findInternalUserIdByExternalUserId(externalUserId)).thenReturn(Optional.of(42L));

        Long result = resolver.resolveInternalUserId(externalUserId);

        assertThat(result).isEqualTo(42L);
        verifyNoInteractions(userRepository);
    }

    @Test
    void fallsBackToUserRepositoryAndCachesResolvedInternalUserId() {
        UUID externalUserId = UUID.randomUUID();
        User user = User.builder()
            .externalUserId(externalUserId)
            .internalUserId(42L)
            .build();
        UserIdResolver resolver = new UserIdResolver(userIdCacheRepository, userRepository);

        when(userIdCacheRepository.findInternalUserIdByExternalUserId(externalUserId)).thenReturn(Optional.empty());
        when(userRepository.findById(externalUserId)).thenReturn(Optional.of(user));

        Long result = resolver.resolveInternalUserId(externalUserId);

        assertThat(result).isEqualTo(42L);
        verify(userIdCacheRepository).save(externalUserId, 42L);
    }

    @Test
    void throwsWhenExternalUserIdDoesNotExist() {
        UUID externalUserId = UUID.randomUUID();
        UserIdResolver resolver = new UserIdResolver(userIdCacheRepository, userRepository);

        when(userIdCacheRepository.findInternalUserIdByExternalUserId(externalUserId)).thenReturn(Optional.empty());
        when(userRepository.findById(externalUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveInternalUserId(externalUserId))
            .isInstanceOf(DomainException.class);
    }
}
