package depth.finvibe.modules.market.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CategoryDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChangeRateResponse {
        private Long categoryId;
        private String categoryName;
        private BigDecimal averageChangePct;
        private Integer stockCount;
        private Integer positiveCount;
        private Integer negativeCount;
        private LocalDateTime updatedAt;
    }
}
