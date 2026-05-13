package depth.finvibe.modules.asset.application.dto;

import java.time.Instant;

public record PortfolioValuationSnapshot(
    Long portfolioId,
    Long purchasedValue,
    Long currentValue,
    Double profitRate,
    Long assetCount,
    Instant updatedAt
) {
}
