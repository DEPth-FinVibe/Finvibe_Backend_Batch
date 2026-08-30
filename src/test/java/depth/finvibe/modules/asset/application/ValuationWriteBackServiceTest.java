package depth.finvibe.modules.asset.application;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import depth.finvibe.modules.asset.application.dto.PortfolioValuationSnapshot;
import depth.finvibe.modules.asset.application.dto.UserValuationSnapshot;
import depth.finvibe.modules.asset.application.port.out.PortfolioValuationRepository;
import depth.finvibe.modules.asset.application.port.out.UserValuationRepository;
import depth.finvibe.modules.asset.application.port.out.ValuationDirtyRepository;
import depth.finvibe.modules.asset.application.port.out.ValuationSnapshotRepository;

@ExtendWith(MockitoExtension.class)
class ValuationWriteBackServiceTest {
    private static final int BATCH_SIZE = 500;

    @Mock
    private ValuationDirtyRepository valuationDirtyRepository;

    @Mock
    private ValuationSnapshotRepository valuationSnapshotRepository;

    @Mock
    private PortfolioValuationRepository portfolioValuationRepository;

    @Mock
    private UserValuationRepository userValuationRepository;

    private ValuationWriteBackService service;

    @BeforeEach
    void setUp() {
        service = new ValuationWriteBackService(
            valuationDirtyRepository,
            valuationSnapshotRepository,
            new ValuationWriteBackTransaction(portfolioValuationRepository, userValuationRepository),
            new SimpleMeterRegistry()
        );
        ReflectionTestUtils.setField(service, "portfolioBatchSize", BATCH_SIZE);
        ReflectionTestUtils.setField(service, "userBatchSize", BATCH_SIZE);
        ReflectionTestUtils.setField(service, "deletionBatchSize", BATCH_SIZE);
    }

    @Test
    void processesDeletesBeforePortfolioAndUserUpsertsAndRemovesDirtyAfterSuccess() {
        Instant deletedAt = Instant.parse("2026-05-14T00:00:00Z");
        PortfolioValuationSnapshot portfolioSnapshot = new PortfolioValuationSnapshot(
            2L,
            1000L,
            1200L,
            20.0,
            3L,
            Instant.parse("2026-05-14T00:01:00Z")
        );
        UserValuationSnapshot userSnapshot = new UserValuationSnapshot(
            "user-1",
            2000L,
            2400L,
            20.0,
            2L,
            Instant.parse("2026-05-14T00:02:00Z")
        );

        when(valuationDirtyRepository.scanPortfolioValuationDeletionDirty(BATCH_SIZE)).thenReturn(List.of(1L));
        when(valuationSnapshotRepository.readPortfolioDeletedAt(1L)).thenReturn(Optional.of(deletedAt));
        when(valuationDirtyRepository.scanPortfolioValuationDirty(BATCH_SIZE)).thenReturn(List.of(2L));
        when(valuationSnapshotRepository.isPortfolioDeleted(2L)).thenReturn(false);
        when(valuationSnapshotRepository.readPortfolioSnapshot(2L)).thenReturn(Optional.of(portfolioSnapshot));
        when(valuationDirtyRepository.scanUserValuationDirty(BATCH_SIZE)).thenReturn(List.of("user-1"));
        when(valuationSnapshotRepository.readUserSnapshot("user-1")).thenReturn(Optional.of(userSnapshot));

        service.runWriteBackBatch();

        InOrder ordered = inOrder(
            valuationDirtyRepository,
            valuationSnapshotRepository,
            portfolioValuationRepository,
            userValuationRepository
        );
        ordered.verify(valuationDirtyRepository).scanPortfolioValuationDeletionDirty(BATCH_SIZE);
        ordered.verify(portfolioValuationRepository).softDeleteIfNewer(1L, deletedAt);
        ordered.verify(valuationDirtyRepository).removePortfolioValuationDeletionDirty(1L);
        ordered.verify(valuationDirtyRepository).removePortfolioValuationDirty(1L);
        ordered.verify(valuationDirtyRepository).scanPortfolioValuationDirty(BATCH_SIZE);
        ordered.verify(portfolioValuationRepository).upsertIfNewer(portfolioSnapshot);
        ordered.verify(valuationDirtyRepository).removePortfolioValuationDirty(2L);
        ordered.verify(valuationDirtyRepository).scanUserValuationDirty(BATCH_SIZE);
        ordered.verify(userValuationRepository).upsertIfNewer(userSnapshot);
        ordered.verify(valuationDirtyRepository).removeUserValuationDirty("user-1");
    }

    @Test
    void keepsPortfolioDirtyWhenRequiredSnapshotIsMissing() {
        when(valuationDirtyRepository.scanPortfolioValuationDeletionDirty(BATCH_SIZE)).thenReturn(List.of());
        when(valuationDirtyRepository.scanPortfolioValuationDirty(BATCH_SIZE)).thenReturn(List.of(1L));
        when(valuationSnapshotRepository.isPortfolioDeleted(1L)).thenReturn(false);
        when(valuationSnapshotRepository.readPortfolioSnapshot(1L)).thenReturn(Optional.empty());
        when(valuationDirtyRepository.scanUserValuationDirty(BATCH_SIZE)).thenReturn(List.of());

        service.runWriteBackBatch();

        verify(portfolioValuationRepository, never()).upsertIfNewer(org.mockito.ArgumentMatchers.any());
        verify(valuationDirtyRepository, never()).removePortfolioValuationDirty(1L);
    }

    @Test
    void keepsUserDirtyWhenDbUpsertFails() {
        UserValuationSnapshot userSnapshot = new UserValuationSnapshot(
            "user-1",
            1000L,
            900L,
            -10.0,
            1L,
            Instant.parse("2026-05-14T00:00:00Z")
        );
        when(valuationDirtyRepository.scanPortfolioValuationDeletionDirty(BATCH_SIZE)).thenReturn(List.of());
        when(valuationDirtyRepository.scanPortfolioValuationDirty(BATCH_SIZE)).thenReturn(List.of());
        when(valuationDirtyRepository.scanUserValuationDirty(BATCH_SIZE)).thenReturn(List.of("user-1"));
        when(valuationSnapshotRepository.readUserSnapshot("user-1")).thenReturn(Optional.of(userSnapshot));
        org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
            .when(userValuationRepository)
            .upsertIfNewer(userSnapshot);

        service.runWriteBackBatch();

        verify(valuationDirtyRepository, never()).removeUserValuationDirty("user-1");
    }

    @Test
    void keepsDeletionDirtyWhenDeletedAtIsMissing() {
        when(valuationDirtyRepository.scanPortfolioValuationDeletionDirty(BATCH_SIZE)).thenReturn(List.of(1L));
        when(valuationSnapshotRepository.readPortfolioDeletedAt(1L)).thenReturn(Optional.empty());
        when(valuationDirtyRepository.scanPortfolioValuationDirty(BATCH_SIZE)).thenReturn(List.of());
        when(valuationDirtyRepository.scanUserValuationDirty(BATCH_SIZE)).thenReturn(List.of());

        service.runWriteBackBatch();

        verify(portfolioValuationRepository, never()).softDeleteIfNewer(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(valuationDirtyRepository, never()).removePortfolioValuationDeletionDirty(1L);
        verify(valuationDirtyRepository, never()).removePortfolioValuationDirty(1L);
    }

    @Test
    void skipsDeletedPortfolioUpsertWithoutRemovingPortfolioDirty() {
        when(valuationDirtyRepository.scanPortfolioValuationDeletionDirty(BATCH_SIZE)).thenReturn(List.of());
        when(valuationDirtyRepository.scanPortfolioValuationDirty(BATCH_SIZE)).thenReturn(List.of(1L));
        when(valuationSnapshotRepository.isPortfolioDeleted(1L)).thenReturn(true);
        when(valuationDirtyRepository.scanUserValuationDirty(BATCH_SIZE)).thenReturn(List.of());

        service.runWriteBackBatch();

        verify(valuationSnapshotRepository, never()).readPortfolioSnapshot(1L);
        verify(portfolioValuationRepository, never()).upsertIfNewer(org.mockito.ArgumentMatchers.any());
        verify(valuationDirtyRepository, never()).removePortfolioValuationDirty(1L);
    }

    @Test
    void drainsMultipleFullPortfolioChunksWithinOneRun() {
        ReflectionTestUtils.setField(service, "portfolioBatchSize", 2);
        PortfolioValuationSnapshot first = portfolioSnapshot(1L);
        PortfolioValuationSnapshot second = portfolioSnapshot(2L);
        PortfolioValuationSnapshot third = portfolioSnapshot(3L);
        when(valuationDirtyRepository.scanPortfolioValuationDeletionDirty(BATCH_SIZE)).thenReturn(List.of());
        when(valuationDirtyRepository.scanPortfolioValuationDirty(2))
            .thenReturn(List.of(1L, 2L), List.of(3L));
        when(valuationSnapshotRepository.readPortfolioSnapshot(1L)).thenReturn(Optional.of(first));
        when(valuationSnapshotRepository.readPortfolioSnapshot(2L)).thenReturn(Optional.of(second));
        when(valuationSnapshotRepository.readPortfolioSnapshot(3L)).thenReturn(Optional.of(third));
        when(valuationDirtyRepository.scanUserValuationDirty(BATCH_SIZE)).thenReturn(List.of());

        service.runWriteBackBatch();

        verify(valuationDirtyRepository, times(2)).scanPortfolioValuationDirty(2);
        verify(valuationDirtyRepository).removePortfolioValuationDirty(1L);
        verify(valuationDirtyRepository).removePortfolioValuationDirty(2L);
        verify(valuationDirtyRepository).removePortfolioValuationDirty(3L);
    }

    @Test
    void keepsWholePortfolioChunkDirtyWhenOneUpsertFails() {
        PortfolioValuationSnapshot first = portfolioSnapshot(1L);
        PortfolioValuationSnapshot second = portfolioSnapshot(2L);
        when(valuationDirtyRepository.scanPortfolioValuationDeletionDirty(BATCH_SIZE)).thenReturn(List.of());
        when(valuationDirtyRepository.scanPortfolioValuationDirty(BATCH_SIZE)).thenReturn(List.of(1L, 2L));
        when(valuationSnapshotRepository.readPortfolioSnapshot(1L)).thenReturn(Optional.of(first));
        when(valuationSnapshotRepository.readPortfolioSnapshot(2L)).thenReturn(Optional.of(second));
        when(valuationDirtyRepository.scanUserValuationDirty(BATCH_SIZE)).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new IllegalStateException("db down"))
            .when(portfolioValuationRepository)
            .upsertIfNewer(second);

        service.runWriteBackBatch();

        verify(valuationDirtyRepository, never()).removePortfolioValuationDirty(1L);
        verify(valuationDirtyRepository, never()).removePortfolioValuationDirty(2L);
    }

    private PortfolioValuationSnapshot portfolioSnapshot(Long portfolioId) {
        return new PortfolioValuationSnapshot(
            portfolioId,
            1000L,
            1200L,
            20.0,
            3L,
            Instant.parse("2026-05-14T00:01:00Z")
        );
    }
}
