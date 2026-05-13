package depth.finvibe.modules.asset.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.dto.PortfolioValuationSnapshot;
import depth.finvibe.modules.asset.application.dto.UserValuationSnapshot;
import depth.finvibe.modules.asset.application.port.out.PortfolioValuationRepository;
import depth.finvibe.modules.asset.application.port.out.UserValuationRepository;
import depth.finvibe.modules.asset.application.port.out.ValuationDirtyRepository;
import depth.finvibe.modules.asset.application.port.out.ValuationSnapshotRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ValuationWriteBackService {
    private final ValuationDirtyRepository valuationDirtyRepository;
    private final ValuationSnapshotRepository valuationSnapshotRepository;
    private final PortfolioValuationRepository portfolioValuationRepository;
    private final UserValuationRepository userValuationRepository;
    private final MeterRegistry meterRegistry;

    @Value("${batch.write-back.portfolio-size:500}")
    private int portfolioBatchSize;

    @Value("${batch.write-back.user-size:500}")
    private int userBatchSize;

    @Value("${batch.write-back.deletion-size:500}")
    private int deletionBatchSize;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void runWriteBackBatch() {
        Instant startedAt = Instant.now();
        processPortfolioDeletes();
        processPortfolioUpserts();
        processUserUpserts();
        meterRegistry.timer("valuation.write_back.duration")
            .record(Duration.between(startedAt, Instant.now()));
    }

    private void processPortfolioDeletes() {
        for (Long portfolioId : valuationDirtyRepository.scanPortfolioValuationDeletionDirty(deletionBatchSize)) {
            try {
                Optional<Instant> deletedAt = valuationSnapshotRepository.readPortfolioDeletedAt(portfolioId);
                if (deletedAt.isEmpty()) {
                    meterRegistry.counter("valuation.write_back.snapshot_missing", "type", "portfolio_deletion").increment();
                    log.warn("Skip portfolio valuation soft delete because deletedAt is missing. portfolioId={}", portfolioId);
                    continue;
                }

                portfolioValuationRepository.softDeleteIfNewer(portfolioId, deletedAt.get());
                valuationDirtyRepository.removePortfolioValuationDeletionDirty(portfolioId);
                valuationDirtyRepository.removePortfolioValuationDirty(portfolioId);
                meterRegistry.counter("valuation.write_back.success", "type", "portfolio_deletion").increment();
            } catch (Exception ex) {
                meterRegistry.counter("valuation.write_back.failure", "type", "portfolio_deletion").increment();
                log.error("Failed to write back portfolio valuation deletion. portfolioId={}", portfolioId, ex);
            }
        }
    }

    private void processPortfolioUpserts() {
        for (Long portfolioId : valuationDirtyRepository.scanPortfolioValuationDirty(portfolioBatchSize)) {
            try {
                if (valuationSnapshotRepository.isPortfolioDeleted(portfolioId)) {
                    log.debug("Skip deleted portfolio valuation upsert. portfolioId={}", portfolioId);
                    continue;
                }

                Optional<PortfolioValuationSnapshot> snapshot = valuationSnapshotRepository.readPortfolioSnapshot(portfolioId);
                if (snapshot.isEmpty()) {
                    meterRegistry.counter("valuation.write_back.snapshot_missing", "type", "portfolio").increment();
                    continue;
                }

                portfolioValuationRepository.upsertIfNewer(snapshot.get());
                valuationDirtyRepository.removePortfolioValuationDirty(portfolioId);
                meterRegistry.counter("valuation.write_back.success", "type", "portfolio").increment();
            } catch (Exception ex) {
                meterRegistry.counter("valuation.write_back.failure", "type", "portfolio").increment();
                log.error("Failed to write back portfolio valuation. portfolioId={}", portfolioId, ex);
            }
        }
    }

    private void processUserUpserts() {
        for (String userId : valuationDirtyRepository.scanUserValuationDirty(userBatchSize)) {
            try {
                Optional<UserValuationSnapshot> snapshot = valuationSnapshotRepository.readUserSnapshot(userId);
                if (snapshot.isEmpty()) {
                    meterRegistry.counter("valuation.write_back.snapshot_missing", "type", "user").increment();
                    continue;
                }

                userValuationRepository.upsertIfNewer(snapshot.get());
                valuationDirtyRepository.removeUserValuationDirty(userId);
                meterRegistry.counter("valuation.write_back.success", "type", "user").increment();
            } catch (Exception ex) {
                meterRegistry.counter("valuation.write_back.failure", "type", "user").increment();
                log.error("Failed to write back user valuation. userId={}", userId, ex);
            }
        }
    }
}
