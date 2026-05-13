package depth.finvibe.modules.asset.application.dto;

import java.time.Instant;

public record UserValuationSnapshot(
    String userId,
    Long purchasedValue,
    Long currentValue,
    Double profitRate,
    Long portfolioCount,
    Instant updatedAt
) {
}
