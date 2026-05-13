package depth.finvibe.modules.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import depth.finvibe.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.modules.asset.application.port.out.PortfolioPerformanceSnapshotRepository;
import depth.finvibe.modules.asset.domain.PortfolioGroup;
import depth.finvibe.modules.asset.domain.PortfolioValuation;
import depth.finvibe.modules.user.application.service.UserIdResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PortfolioPerformanceSnapshotServiceTest {

    @Mock
    private PortfolioGroupRepository portfolioGroupRepository;

    @Mock
    private PortfolioPerformanceSnapshotRepository portfolioPerformanceSnapshotRepository;

    @Mock
    private UserIdResolver userIdResolver;

    @Test
    void persistsInternalLongUserIdInPortfolioPerformanceSnapshot() {
        UUID externalUserId = UUID.randomUUID();
        PortfolioGroup portfolio = PortfolioGroup.builder()
            .id(1L)
            .userId(externalUserId)
            .name("테스트 포트폴리오")
            .valuation(PortfolioValuation.empty())
            .build();
        PortfolioPerformanceSnapshotService service = new PortfolioPerformanceSnapshotService(
            portfolioGroupRepository,
            portfolioPerformanceSnapshotRepository,
            userIdResolver
        );

        when(portfolioGroupRepository.findPortfolioIdsAfter(null, 0)).thenReturn(List.of(1L));
        when(portfolioGroupRepository.findPortfolioIdsAfter(1L, 0)).thenReturn(List.of());
        when(portfolioGroupRepository.findAllWithAssetsByIds(List.of(1L))).thenReturn(List.of(portfolio));
        when(userIdResolver.resolveInternalUserIdIfPresent(externalUserId)).thenReturn(Optional.of(42L));

        service.saveDailySnapshot(LocalDate.of(2026, 5, 13));

        verify(portfolioPerformanceSnapshotRepository).saveAll(argThat(snapshots -> {
            assertThat(snapshots).hasSize(1);
            assertThat(snapshots.getFirst().getUserId()).isEqualTo(42L);
            return true;
        }));
    }
}
