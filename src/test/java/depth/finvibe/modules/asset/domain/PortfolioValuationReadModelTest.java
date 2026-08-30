package depth.finvibe.modules.asset.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class PortfolioValuationReadModelTest {
    @Test
    void updatesOnlyWhenSnapshotIsNewer() {
        PortfolioValuationReadModel valuation = PortfolioValuationReadModel.create(
            1L,
            1000L,
            1100L,
            10.0,
            1L,
            Instant.parse("2026-05-14T00:00:00Z")
        );

        valuation.updateIfNewer(1000L, 1300L, 30.0, 2L, Instant.parse("2026-05-13T23:59:59Z"));

        assertThat(valuation.getCurrentValue()).isEqualTo(1100L);
        assertThat(valuation.getProfitRate()).isEqualTo(10.0);

        valuation.updateIfNewer(1000L, 1300L, 30.0, 2L, Instant.parse("2026-05-14T00:00:01Z"));

        assertThat(valuation.getCurrentValue()).isEqualTo(1300L);
        assertThat(valuation.getProfitRate()).isEqualTo(30.0);
        assertThat(valuation.getAssetCount()).isEqualTo(2L);
    }

    @Test
    void softDeletedPortfolioIsNotRevivedByNormalUpsert() {
        PortfolioValuationReadModel valuation = PortfolioValuationReadModel.create(
            1L,
            1000L,
            1100L,
            10.0,
            1L,
            Instant.parse("2026-05-14T00:00:00Z")
        );

        valuation.softDeleteIfNewer(Instant.parse("2026-05-14T00:01:00Z"));
        valuation.updateIfNewer(1000L, 1500L, 50.0, 3L, Instant.parse("2026-05-14T00:02:00Z"));

        assertThat(valuation.isDeleted()).isTrue();
        assertThat(valuation.getDeletedAt()).isEqualTo(Instant.parse("2026-05-14T00:01:00Z"));
        assertThat(valuation.getCurrentValue()).isEqualTo(1100L);
        assertThat(valuation.getProfitRate()).isEqualTo(10.0);
    }

    @Test
    void ignoresDeletionOlderThanLatestValuation() {
        PortfolioValuationReadModel valuation = PortfolioValuationReadModel.create(
            1L,
            1000L,
            1100L,
            10.0,
            1L,
            Instant.parse("2026-05-14T00:01:00Z")
        );

        valuation.softDeleteIfNewer(Instant.parse("2026-05-14T00:00:59Z"));

        assertThat(valuation.isDeleted()).isFalse();
        assertThat(valuation.getDeletedAt()).isNull();
    }
}
