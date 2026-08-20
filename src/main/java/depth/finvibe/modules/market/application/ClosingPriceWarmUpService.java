package depth.finvibe.modules.market.application;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import depth.finvibe.modules.market.application.port.out.ClosingPriceRepository;
import depth.finvibe.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.modules.market.application.port.out.StockRepository;
import depth.finvibe.modules.market.domain.ClosingPrice;
import depth.finvibe.modules.market.domain.Stock;
import depth.finvibe.modules.market.domain.enums.TradingDayStatus;
import depth.finvibe.modules.market.dto.PriceCandleDto;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClosingPriceWarmUpService {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");
  private static final int MAX_ATTEMPTS = 3;

  private final StockRepository stockRepository;
  private final ClosingPriceRepository closingPriceRepository;
  private final RealMarketClient realMarketClient;
  private final HolidayCalendarService holidayCalendarService;
  private final MeterRegistry meterRegistry;
  private final AtomicLong lastSuccessEpochSeconds = new AtomicLong(0L);

  @PostConstruct
  void registerMetrics() {
    Gauge.builder("market.closing.price.warmup.last.success.epoch", lastSuccessEpochSeconds, AtomicLong::get)
        .register(meterRegistry);
  }

  public WarmUpResult warmUpScheduledClosingPrices() {
    return warmUpScheduledAt(LocalDateTime.now(KOREA_ZONE));
  }

  public WarmUpResult recoverLatestCompletedClosingPrices() {
    return warmUpLatestCompletedAt(LocalDateTime.now(KOREA_ZONE));
  }

  WarmUpResult warmUpScheduledAt(LocalDateTime now) {
    TradingDayStatus tradingDayStatus = holidayCalendarService.getTradingDayStatus(now.toLocalDate());
    if (tradingDayStatus == TradingDayStatus.CLOSED) {
      meterRegistry.counter("market.closing.price.warmup.skipped", "reason", "closed_day").increment();
      log.info("휴장일이므로 종가 워밍업을 건너뜁니다. date={}", now.toLocalDate());
      return WarmUpResult.skipped(now.toLocalDate(), "CLOSED_DAY");
    }
    if (tradingDayStatus == TradingDayStatus.UNKNOWN) {
      meterRegistry.counter("market.closing.price.warmup.failures", "reason", "calendar_unavailable").increment();
      throw new IllegalStateException("거래일 달력을 확인할 수 없어 종가 워밍업을 중단합니다: " + now.toLocalDate());
    }
    return warmUpLatestCompletedAt(now);
  }

  WarmUpResult warmUpLatestCompletedAt(LocalDateTime now) {
    LocalDate tradingDate = holidayCalendarService.getLastCompletedTradingDay(now)
        .orElseThrow(() -> new IllegalStateException("최근 완료 거래일을 확인할 수 없습니다: " + now));

    Map<Long, Stock> targetStocks = stockRepository.findAll().stream()
        .filter(stock -> stock.getId() != null)
        .filter(stock -> stock.getSymbol() != null && stock.getSymbol().matches("\\d{6}"))
        .collect(Collectors.toMap(Stock::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

    meterRegistry.counter("market.closing.price.warmup.target.stocks").increment(targetStocks.size());
    if (targetStocks.isEmpty()) {
      markSuccess(now);
      return WarmUpResult.completed(tradingDate, 0, 0);
    }

    Set<Long> cachedStockIds = closingPriceRepository
        .findByStockIdsAndTradingDate(List.copyOf(targetStocks.keySet()), tradingDate)
        .stream()
        .map(ClosingPrice::getStockId)
        .collect(Collectors.toSet());

    Map<Long, Stock> remainingStocks = targetStocks.entrySet().stream()
        .filter(entry -> !cachedStockIds.contains(entry.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left, LinkedHashMap::new));

    for (int attempt = 1; attempt <= MAX_ATTEMPTS && !remainingStocks.isEmpty(); attempt++) {
      List<String> symbols = remainingStocks.values().stream().map(Stock::getSymbol).toList();
      List<PriceCandleDto.Response> snapshots;
      try {
        snapshots = realMarketClient.bulkFetchCurrentPrices(symbols);
      } catch (RuntimeException exception) {
        meterRegistry.counter("market.closing.price.warmup.attempt.failures").increment();
        log.warn(
            "종가 워밍업 외부 조회가 실패했습니다. tradingDate={}, attempt={}, targetCount={}",
            tradingDate,
            attempt,
            symbols.size(),
            exception
        );
        continue;
      }

      Map<Long, PriceCandleDto.Response> snapshotByStockId = snapshots.stream()
          .filter(snapshot -> snapshot.getStockId() != null && remainingStocks.containsKey(snapshot.getStockId()))
          .filter(snapshot -> snapshot.getClose() != null)
          .collect(Collectors.toMap(
              PriceCandleDto.Response::getStockId,
              Function.identity(),
              (first, duplicate) -> first,
              LinkedHashMap::new
          ));
      List<ClosingPrice> fetchedPrices = snapshotByStockId.values().stream()
          .map(snapshot -> ClosingPrice.create(
              snapshot.getStockId(),
              tradingDate,
              tradingDate.atTime(LocalTime.of(15, 30)),
              snapshot.getClose(),
              snapshot.getPrevDayChangePct(),
              snapshot.getVolume(),
              snapshot.getValue()
          ))
          .toList();

      closingPriceRepository.saveAll(fetchedPrices);
      fetchedPrices.forEach(price -> remainingStocks.remove(price.getStockId()));
    }

    int succeededCount = targetStocks.size() - remainingStocks.size();
    meterRegistry.counter("market.closing.price.warmup.succeeded.stocks").increment(succeededCount);
    if (!remainingStocks.isEmpty()) {
      meterRegistry.counter("market.closing.price.warmup.failed.stocks").increment(remainingStocks.size());
      log.error(
          "종가 워밍업이 일부 종목을 확보하지 못했습니다. tradingDate={}, targetCount={}, successCount={}, failedCount={}, failedStockIds={}",
          tradingDate,
          targetStocks.size(),
          succeededCount,
          remainingStocks.size(),
          remainingStocks.keySet().stream().limit(30).toList()
      );
      throw new IncompleteWarmUpException(tradingDate, List.copyOf(remainingStocks.keySet()));
    }

    markSuccess(now);
    log.info("종가 워밍업을 완료했습니다. tradingDate={}, targetCount={}", tradingDate, targetStocks.size());
    return WarmUpResult.completed(tradingDate, targetStocks.size(), succeededCount);
  }

  private void markSuccess(LocalDateTime now) {
    lastSuccessEpochSeconds.set(now.atZone(KOREA_ZONE).toEpochSecond());
  }

  public record WarmUpResult(
      LocalDate tradingDate,
      int targetCount,
      int succeededCount,
      boolean skipped,
      String skipReason
  ) {
    static WarmUpResult completed(LocalDate tradingDate, int targetCount, int succeededCount) {
      return new WarmUpResult(tradingDate, targetCount, succeededCount, false, null);
    }

    static WarmUpResult skipped(LocalDate date, String reason) {
      return new WarmUpResult(date, 0, 0, true, reason);
    }
  }

  @Getter
  public static class IncompleteWarmUpException extends RuntimeException {
    private final LocalDate tradingDate;
    private final List<Long> missingStockIds;

    public IncompleteWarmUpException(LocalDate tradingDate, List<Long> missingStockIds) {
      super("종가 워밍업이 완료되지 않았습니다. tradingDate=" + tradingDate + ", missingCount=" + missingStockIds.size());
      this.tradingDate = tradingDate;
      this.missingStockIds = missingStockIds;
    }
  }
}
