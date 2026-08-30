package depth.finvibe.modules.asset.application;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.asset.application.ValuationWriteBackTransaction.PortfolioDeletion;
import depth.finvibe.modules.asset.application.dto.PortfolioValuationSnapshot;
import depth.finvibe.modules.asset.application.dto.UserValuationSnapshot;
import depth.finvibe.modules.asset.application.port.out.ValuationDirtyRepository;
import depth.finvibe.modules.asset.application.port.out.ValuationSnapshotRepository;

@Slf4j
@Service
public class ValuationWriteBackService {
    private final ValuationDirtyRepository valuationDirtyRepository;
    private final ValuationSnapshotRepository valuationSnapshotRepository;
    private final ValuationWriteBackTransaction writeBackTransaction;
    private final MeterRegistry meterRegistry;
    private final AtomicLong portfolioBacklog = new AtomicLong();
    private final AtomicLong userBacklog = new AtomicLong();
    private final AtomicLong deletionBacklog = new AtomicLong();
    private final AtomicLong portfolioOldestAgeSeconds = new AtomicLong();
    private final AtomicLong userOldestAgeSeconds = new AtomicLong();
    private final AtomicLong deletionOldestAgeSeconds = new AtomicLong();

    @Value("${batch.write-back.portfolio-size:500}")
    private int portfolioBatchSize;

    @Value("${batch.write-back.user-size:500}")
    private int userBatchSize;

    @Value("${batch.write-back.deletion-size:500}")
    private int deletionBatchSize;

    @Value("${batch.write-back.max-duration:PT25S}")
    private Duration maxRunDuration = Duration.ofSeconds(25);

    public ValuationWriteBackService(
        ValuationDirtyRepository valuationDirtyRepository,
        ValuationSnapshotRepository valuationSnapshotRepository,
        ValuationWriteBackTransaction writeBackTransaction,
        MeterRegistry meterRegistry
    ) {
        this.valuationDirtyRepository = valuationDirtyRepository;
        this.valuationSnapshotRepository = valuationSnapshotRepository;
        this.writeBackTransaction = writeBackTransaction;
        this.meterRegistry = meterRegistry;
        registerGauge("portfolio", portfolioBacklog, portfolioOldestAgeSeconds);
        registerGauge("user", userBacklog, userOldestAgeSeconds);
        registerGauge("portfolio_deletion", deletionBacklog, deletionOldestAgeSeconds);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void runWriteBackBatch() {
        Instant startedAt = Instant.now();
        Instant deadline = startedAt.plus(maxRunDuration);
        resetOldestAges();
        try {
            drainDeletes(deadline);
            drainPortfolios(deadline);
            drainUsers(deadline);
        } finally {
            refreshBacklogMetrics();
            meterRegistry.timer("valuation.write_back.duration")
                .record(Duration.between(startedAt, Instant.now()));
        }
    }

    private void drainDeletes(Instant deadline) {
        BatchOutcome outcome;
        do {
            outcome = processPortfolioDeletes();
        } while (shouldContinue(outcome, deletionBatchSize, deadline));
    }

    private void drainPortfolios(Instant deadline) {
        BatchOutcome outcome;
        do {
            outcome = processPortfolioUpserts();
        } while (shouldContinue(outcome, portfolioBatchSize, deadline));
    }

    private void drainUsers(Instant deadline) {
        BatchOutcome outcome;
        do {
            outcome = processUserUpserts();
        } while (shouldContinue(outcome, userBatchSize, deadline));
    }

    private BatchOutcome processPortfolioDeletes() {
        List<Long> dirtyIds = valuationDirtyRepository.scanPortfolioValuationDeletionDirty(deletionBatchSize);
        List<PortfolioDeletion> deletions = new ArrayList<>(dirtyIds.size());
        for (Long portfolioId : dirtyIds) {
            try {
                Optional<Instant> deletedAt = valuationSnapshotRepository.readPortfolioDeletedAt(portfolioId);
                if (deletedAt.isEmpty()) {
                    recordMissing("portfolio_deletion");
                    continue;
                }
                updateOldestAge(deletionOldestAgeSeconds, deletedAt.get());
                deletions.add(new PortfolioDeletion(portfolioId, deletedAt.get()));
            } catch (Exception ex) {
                recordFailure("portfolio_deletion", portfolioId, ex);
            }
        }
        if (deletions.isEmpty()) {
            return new BatchOutcome(dirtyIds.size(), 0);
        }
        try {
            writeBackTransaction.deletePortfolios(deletions);
            deletions.forEach(deletion -> {
                valuationDirtyRepository.removePortfolioValuationDeletionDirty(deletion.portfolioId());
                valuationDirtyRepository.removePortfolioValuationDirty(deletion.portfolioId());
            });
            recordSuccess("portfolio_deletion", deletions.size());
            return new BatchOutcome(dirtyIds.size(), deletions.size());
        } catch (Exception ex) {
            recordFailure("portfolio_deletion", "chunk", ex);
            return new BatchOutcome(dirtyIds.size(), 0);
        }
    }

    private BatchOutcome processPortfolioUpserts() {
        List<Long> dirtyIds = valuationDirtyRepository.scanPortfolioValuationDirty(portfolioBatchSize);
        List<PortfolioValuationSnapshot> snapshots = new ArrayList<>(dirtyIds.size());
        for (Long portfolioId : dirtyIds) {
            try {
                if (valuationSnapshotRepository.isPortfolioDeleted(portfolioId)) {
                    continue;
                }
                Optional<PortfolioValuationSnapshot> snapshot =
                    valuationSnapshotRepository.readPortfolioSnapshot(portfolioId);
                if (snapshot.isEmpty()) {
                    recordMissing("portfolio");
                    continue;
                }
                updateOldestAge(portfolioOldestAgeSeconds, snapshot.get().updatedAt());
                snapshots.add(snapshot.get());
            } catch (Exception ex) {
                recordFailure("portfolio", portfolioId, ex);
            }
        }
        if (snapshots.isEmpty()) {
            return new BatchOutcome(dirtyIds.size(), 0);
        }
        try {
            writeBackTransaction.upsertPortfolios(snapshots);
            snapshots.forEach(snapshot ->
                valuationDirtyRepository.removePortfolioValuationDirty(snapshot.portfolioId()));
            recordSuccess("portfolio", snapshots.size());
            return new BatchOutcome(dirtyIds.size(), snapshots.size());
        } catch (Exception ex) {
            recordFailure("portfolio", "chunk", ex);
            return new BatchOutcome(dirtyIds.size(), 0);
        }
    }

    private BatchOutcome processUserUpserts() {
        List<String> dirtyIds = valuationDirtyRepository.scanUserValuationDirty(userBatchSize);
        List<UserValuationSnapshot> snapshots = new ArrayList<>(dirtyIds.size());
        for (String userId : dirtyIds) {
            try {
                Optional<UserValuationSnapshot> snapshot = valuationSnapshotRepository.readUserSnapshot(userId);
                if (snapshot.isEmpty()) {
                    recordMissing("user");
                    continue;
                }
                updateOldestAge(userOldestAgeSeconds, snapshot.get().updatedAt());
                snapshots.add(snapshot.get());
            } catch (Exception ex) {
                recordFailure("user", userId, ex);
            }
        }
        if (snapshots.isEmpty()) {
            return new BatchOutcome(dirtyIds.size(), 0);
        }
        try {
            writeBackTransaction.upsertUsers(snapshots);
            snapshots.forEach(snapshot ->
                valuationDirtyRepository.removeUserValuationDirty(snapshot.userId()));
            recordSuccess("user", snapshots.size());
            return new BatchOutcome(dirtyIds.size(), snapshots.size());
        } catch (Exception ex) {
            recordFailure("user", "chunk", ex);
            return new BatchOutcome(dirtyIds.size(), 0);
        }
    }

    private boolean shouldContinue(BatchOutcome outcome, int batchSize, Instant deadline) {
        return outcome.scanned() >= batchSize && outcome.committed() > 0 && Instant.now().isBefore(deadline);
    }

    private void refreshBacklogMetrics() {
        try {
            setBacklog(portfolioBacklog, portfolioOldestAgeSeconds,
                valuationDirtyRepository.countPortfolioValuationDirty());
            setBacklog(userBacklog, userOldestAgeSeconds,
                valuationDirtyRepository.countUserValuationDirty());
            setBacklog(deletionBacklog, deletionOldestAgeSeconds,
                valuationDirtyRepository.countPortfolioValuationDeletionDirty());
        } catch (Exception ex) {
            meterRegistry.counter("valuation.write_back.failure", "type", "backlog_metrics").increment();
            log.warn("Failed to read valuation dirty backlog", ex);
        }
    }

    private void setBacklog(AtomicLong backlog, AtomicLong oldestAge, long count) {
        backlog.set(count);
        if (count == 0L) {
            oldestAge.set(0L);
        }
    }

    private void registerGauge(String type, AtomicLong backlog, AtomicLong oldestAge) {
        meterRegistry.gauge("valuation.write_back.backlog", Tags.of("type", type), backlog, AtomicLong::get);
        meterRegistry.gauge("valuation.write_back.oldest_age_seconds", Tags.of("type", type), oldestAge, AtomicLong::get);
    }

    private void resetOldestAges() {
        portfolioOldestAgeSeconds.set(0L);
        userOldestAgeSeconds.set(0L);
        deletionOldestAgeSeconds.set(0L);
    }

    private void updateOldestAge(AtomicLong target, Instant updatedAt) {
        long ageSeconds = Math.max(0L, Duration.between(updatedAt, Instant.now()).toSeconds());
        target.accumulateAndGet(ageSeconds, Math::max);
    }

    private void recordMissing(String type) {
        meterRegistry.counter("valuation.write_back.snapshot_missing", "type", type).increment();
    }

    private void recordSuccess(String type, int count) {
        meterRegistry.counter("valuation.write_back.success", "type", type).increment(count);
    }

    private void recordFailure(String type, Object id, Exception ex) {
        meterRegistry.counter("valuation.write_back.failure", "type", type).increment();
        log.error("Failed to write back valuation. type={}, id={}", type, id, ex);
    }

    private record BatchOutcome(int scanned, int committed) {
    }
}
