package depth.finvibe.modules.asset.application.port.out;

import java.math.BigDecimal;
import java.util.UUID;

public record UserProfitRankingData(
    Long userId,
    String userNickname,
    BigDecimal totalReturnRate,
    BigDecimal totalProfitLoss
) {}
