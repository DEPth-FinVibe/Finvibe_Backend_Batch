package depth.finvibe.modules.asset.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PortfolioWarmUpState(
    Long portfolioId,
    String userId,
    Long purchasedValue,
    Long assetCount,
    Instant updatedAt,
    List<Holding> holdings
) {
    public record Holding(
        Long stockId,
        BigDecimal quantity,
        BigDecimal purchasePriceAmount,
        String currency
    ) {
    }
}
