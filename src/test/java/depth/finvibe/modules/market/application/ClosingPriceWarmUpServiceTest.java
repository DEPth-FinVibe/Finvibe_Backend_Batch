package depth.finvibe.modules.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import depth.finvibe.modules.market.application.ClosingPriceWarmUpService.IncompleteWarmUpException;
import depth.finvibe.modules.market.application.ClosingPriceWarmUpService.WarmUpResult;
import depth.finvibe.modules.market.application.port.out.ClosingPriceRepository;
import depth.finvibe.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.modules.market.application.port.out.StockRepository;
import depth.finvibe.modules.market.domain.ClosingPrice;
import depth.finvibe.modules.market.domain.Stock;
import depth.finvibe.modules.market.domain.enums.Timeframe;
import depth.finvibe.modules.market.domain.enums.TradingDayStatus;
import depth.finvibe.modules.market.dto.PriceCandleDto;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClosingPriceWarmUpServiceTest {

  private final StockRepository stockRepository = mock(StockRepository.class);
  private final ClosingPriceRepository closingPriceRepository = mock(ClosingPriceRepository.class);
  private final RealMarketClient realMarketClient = mock(RealMarketClient.class);
  private final HolidayCalendarService holidayCalendarService = mock(HolidayCalendarService.class);
  private SimpleMeterRegistry meterRegistry;
  private ClosingPriceWarmUpService service;

  @BeforeEach
  void setUp() {
    meterRegistry = new SimpleMeterRegistry();
    service = new ClosingPriceWarmUpService(
        stockRepository,
        closingPriceRepository,
        realMarketClient,
        holidayCalendarService,
        meterRegistry
    );
    service.registerMetrics();
  }

  @Test
  @DisplayName("누락된 종목만 다시 조회해 전체 종가 워밍업을 완료한다")
  void warmUpScheduledAt_partialFirstAttempt_retriesMissingStocks() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 21, 15, 40);
    LocalDate tradingDate = now.toLocalDate();
    Stock first = stock(1L, "삼성전자", "005930");
    Stock second = stock(2L, "SK하이닉스", "000660");
    when(holidayCalendarService.getTradingDayStatus(tradingDate)).thenReturn(TradingDayStatus.OPEN);
    when(holidayCalendarService.getLastCompletedTradingDay(now)).thenReturn(Optional.of(tradingDate));
    when(stockRepository.findAll()).thenReturn(List.of(first, second));
    when(closingPriceRepository.findByStockIdsAndTradingDate(List.of(1L, 2L), tradingDate))
        .thenReturn(List.of());
    when(realMarketClient.bulkFetchCurrentPrices(List.of("005930", "000660")))
        .thenReturn(List.of(snapshot(1L, "70000")));
    when(realMarketClient.bulkFetchCurrentPrices(List.of("000660")))
        .thenReturn(List.of(snapshot(2L, "190000")));

    WarmUpResult result = service.warmUpScheduledAt(now);

    assertThat(result.skipped()).isFalse();
    assertThat(result.targetCount()).isEqualTo(2);
    assertThat(result.succeededCount()).isEqualTo(2);
    verify(realMarketClient).bulkFetchCurrentPrices(List.of("005930", "000660"));
    verify(realMarketClient).bulkFetchCurrentPrices(List.of("000660"));
    verify(closingPriceRepository, times(2)).saveAll(org.mockito.ArgumentMatchers.anyList());
    assertThat(meterRegistry.get("market.closing.price.warmup.last.success.epoch").gauge().value())
        .isPositive();
  }

  @Test
  @DisplayName("휴장일에는 외부 시세 조회 없이 워밍업을 건너뛴다")
  void warmUpScheduledAt_closedDay_skips() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 17, 15, 40);
    when(holidayCalendarService.getTradingDayStatus(now.toLocalDate())).thenReturn(TradingDayStatus.CLOSED);

    WarmUpResult result = service.warmUpScheduledAt(now);

    assertThat(result.skipped()).isTrue();
    assertThat(result.skipReason()).isEqualTo("CLOSED_DAY");
    verifyNoInteractions(stockRepository, closingPriceRepository, realMarketClient);
  }

  @Test
  @DisplayName("재시도 후에도 종가가 누락되면 작업을 실패시킨다")
  void warmUpScheduledAt_retryExhausted_throws() {
    LocalDateTime now = LocalDateTime.of(2026, 8, 21, 15, 40);
    LocalDate tradingDate = now.toLocalDate();
    Stock stock = stock(1L, "삼성전자", "005930");
    when(holidayCalendarService.getTradingDayStatus(tradingDate)).thenReturn(TradingDayStatus.OPEN);
    when(holidayCalendarService.getLastCompletedTradingDay(now)).thenReturn(Optional.of(tradingDate));
    when(stockRepository.findAll()).thenReturn(List.of(stock));
    when(closingPriceRepository.findByStockIdsAndTradingDate(List.of(1L), tradingDate)).thenReturn(List.of());
    when(realMarketClient.bulkFetchCurrentPrices(List.of("005930"))).thenReturn(List.of());

    assertThatThrownBy(() -> service.warmUpScheduledAt(now))
        .isInstanceOf(IncompleteWarmUpException.class)
        .satisfies(exception -> assertThat(((IncompleteWarmUpException) exception).getMissingStockIds())
            .containsExactly(1L));
    verify(realMarketClient, times(3)).bulkFetchCurrentPrices(List.of("005930"));
    assertThat(meterRegistry.counter("market.closing.price.warmup.failed.stocks").count()).isEqualTo(1.0);
  }

  private Stock stock(Long id, String name, String symbol) {
    return Stock.builder().id(id).name(name).symbol(symbol).categoryId(1L).build();
  }

  private PriceCandleDto.Response snapshot(Long stockId, String close) {
    BigDecimal price = new BigDecimal(close);
    return PriceCandleDto.Response.builder()
        .stockId(stockId)
        .at(LocalDateTime.of(2026, 8, 21, 15, 30))
        .timeframe(Timeframe.MINUTE)
        .open(price)
        .high(price)
        .low(price)
        .close(price)
        .prevDayChangePct(BigDecimal.ONE)
        .volume(BigDecimal.TEN)
        .value(price.multiply(BigDecimal.TEN))
        .build();
  }
}
