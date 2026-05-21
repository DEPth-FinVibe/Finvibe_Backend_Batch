package depth.finvibe.modules.asset.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import depth.finvibe.modules.asset.application.port.out.PortfolioGroupRepository;
import depth.finvibe.modules.asset.application.port.out.PortfolioPerformanceSnapshotRepository;
import depth.finvibe.modules.asset.application.port.out.PortfolioValuationRepository;
import depth.finvibe.modules.asset.domain.PortfolioGroup;
import depth.finvibe.modules.asset.domain.PortfolioValuationReadModel;
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
    private PortfolioValuationRepository portfolioValuationRepository;

    @Test
    void persistsInternalLongUserIdInPortfolioPerformanceSnapshot() {
        Long userId = 42L;
        PortfolioGroup portfolio = PortfolioGroup.builder()
            .id(1L)
            .userId(userId)
            .name("테스트 포트폴리오")
            .build();
        PortfolioValuationReadModel valuation = PortfolioValuationReadModel.create(
            1L,
            1000L,
            1100L,
            10.0,
            1L,
            null
        );
        PortfolioPerformanceSnapshotService service = new PortfolioPerformanceSnapshotService(
            portfolioGroupRepository,
            portfolioPerformanceSnapshotRepository,
            portfolioValuationRepository,
            null
        );

        when(portfolioValuationRepository.findActivePortfolioIdsAfter(null, 0)).thenReturn(List.of(1L));
        when(portfolioValuationRepository.findActivePortfolioIdsAfter(1L, 0)).thenReturn(List.of());
        when(portfolioValuationRepository.findActiveByPortfolioIds(List.of(1L))).thenReturn(List.of(valuation));
        when(portfolioGroupRepository.findAllWithAssetsByIds(List.of(1L))).thenReturn(List.of(portfolio));

        service.saveDailySnapshot(LocalDate.of(2026, 5, 13));

        verify(portfolioPerformanceSnapshotRepository).saveAll(argThat(snapshots -> {
            assertThat(snapshots).hasSize(1);
            assertThat(snapshots.getFirst().getUserId()).isEqualTo(userId);
            return true;
        }));
    }
}
