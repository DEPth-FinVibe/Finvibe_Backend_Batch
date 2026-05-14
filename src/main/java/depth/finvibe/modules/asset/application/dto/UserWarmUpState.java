package depth.finvibe.modules.asset.application.dto;

import java.time.Instant;
import java.util.List;

public record UserWarmUpState(
    String userId,
    Long purchasedValue,
    Long currentValue,
    Double profitRate,
    Long portfolioCount,
    Instant updatedAt,
    List<Long> portfolioIds
) {
}
