package depth.finvibe.modules.market.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import depth.finvibe.modules.market.domain.PriceCandle;
import depth.finvibe.modules.market.domain.enums.Timeframe;

public class PriceCandleDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private BigDecimal open;
        private BigDecimal close;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal volume;
        private BigDecimal value;
        private Long stockId;
        private Timeframe timeframe;
        private LocalDateTime at;
        private BigDecimal prevDayChangePct;

        public static Response from(PriceCandle priceCandle) {
            return Response.builder()
                    .open(priceCandle.getOpen())
                    .close(priceCandle.getClose())
                    .high(priceCandle.getHigh())
                    .low(priceCandle.getLow())
                    .volume(priceCandle.getVolume())
                    .value(priceCandle.getValue())
                    .stockId(priceCandle.getStockId())
                    .timeframe(priceCandle.getTimeframe())
                    .at(priceCandle.getAt())
                    .prevDayChangePct(priceCandle.getPrevDayChangePct())
                    .build();
        }
    }
}
