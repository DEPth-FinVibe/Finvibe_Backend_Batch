package depth.finvibe.modules.market.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import depth.finvibe.modules.market.domain.ClosingPrice;
import depth.finvibe.modules.market.domain.Stock;

public class ClosingPriceDto {

  @Getter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {
    private Long stockId;
    private String stockName;
    private LocalDateTime at;
    private BigDecimal close;
    private BigDecimal prevDayChangePct;
    private BigDecimal volume;
    private BigDecimal value;

    public static Response from(ClosingPrice closingPrice, Stock stock) {
      return Response.builder()
          .stockId(closingPrice.getStockId())
          .stockName(stock.getName())
          .at(closingPrice.getAt())
          .close(closingPrice.getClose())
          .prevDayChangePct(closingPrice.getPrevDayChangePct())
          .volume(closingPrice.getVolume())
          .value(closingPrice.getValue())
          .build();
    }
  }
}
