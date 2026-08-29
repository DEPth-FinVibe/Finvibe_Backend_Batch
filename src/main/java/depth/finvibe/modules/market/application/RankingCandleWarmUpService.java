package depth.finvibe.modules.market.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import depth.finvibe.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.modules.market.application.port.out.StockRankingRepository;
import depth.finvibe.modules.market.domain.PriceCandle;
import depth.finvibe.modules.market.domain.StockRanking;
import depth.finvibe.modules.market.domain.enums.RankType;
import depth.finvibe.modules.market.domain.enums.Timeframe;
import depth.finvibe.modules.market.dto.PriceCandleDto;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 홈 화면에 노출되는 랭킹 상위 종목의 일봉을 미리 적재한다.
 * <p>
 * 홈은 거래대금·거래량·급상승·급하락 각 상위 10종목의 스파크라인을 그리는데,
 * 조회 시점에 일봉이 비어 있으면 조회 API가 사용자 요청 스레드에서 KIS를 호출하게 된다.
 * 랭킹이 갱신되는 주기에 맞춰 미리 채워 두면 조회는 DB만 보고 끝난다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingCandleWarmUpService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    /** 홈 화면 탭과 1:1로 대응한다. 순서는 워밍업 우선순위이기도 하다. */
    private static final List<RankType> HOME_RANK_TYPES = List.of(
            RankType.TOP_VALUE,
            RankType.TOP_VOLUME,
            RankType.TOP_RISING,
            RankType.TOP_FALLING
    );

    private final StockRankingRepository stockRankingRepository;
    private final PriceCandleRepository priceCandleRepository;
    private final RealMarketClient realMarketClient;
    private final HolidayCalendarService holidayCalendarService;
    private final MeterRegistry meterRegistry;

    @Value("${market.candle.warmup.top-n:10}")
    private int topN;

    @Value("${market.candle.warmup.months:3}")
    private int months;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public WarmUpResult warmUpHomeRankingCandles() {
        return warmUpAt(LocalDate.now(KOREA_ZONE));
    }

    WarmUpResult warmUpAt(LocalDate baseDate) {
        List<Long> targetStockIds = collectTopRankedStockIds();
        if (targetStockIds.isEmpty()) {
            meterRegistry.counter("market.candle.warmup.skipped", "reason", "no_ranking").increment();
            log.info("랭킹 데이터가 없어 홈 캔들 워밍업을 건너뜁니다. baseDate={}", baseDate);
            return WarmUpResult.skipped(baseDate, "NO_RANKING");
        }

        LocalDate lastTradingDay = holidayCalendarService.getLastTradingDayOnOrBefore(baseDate).orElse(null);
        if (lastTradingDay == null) {
            meterRegistry.counter("market.candle.warmup.skipped", "reason", "calendar_unavailable").increment();
            log.warn("최근 거래일을 확인할 수 없어 홈 캔들 워밍업을 건너뜁니다. baseDate={}", baseDate);
            return WarmUpResult.skipped(baseDate, "CALENDAR_UNAVAILABLE");
        }

        meterRegistry.counter("market.candle.warmup.target.stocks").increment(targetStockIds.size());

        List<Long> staleStockIds = findStaleStockIds(targetStockIds, lastTradingDay);
        if (staleStockIds.isEmpty()) {
            log.debug("홈 랭킹 종목 일봉이 이미 최신입니다. lastTradingDay={}, targetCount={}",
                    lastTradingDay, targetStockIds.size());
            return WarmUpResult.completed(lastTradingDay, targetStockIds.size(), 0, 0, 0);
        }

        LocalDateTime startTime = lastTradingDay.minusMonths(months).atStartOfDay();
        LocalDateTime endTime = lastTradingDay.atStartOfDay();

        int warmedStocks = 0;
        int savedCandles = 0;
        int failedStocks = 0;

        for (Long stockId : staleStockIds) {
            try {
                int saved = warmUpStock(stockId, startTime, endTime);
                if (saved > 0) {
                    warmedStocks++;
                    savedCandles += saved;
                }
            } catch (RuntimeException exception) {
                failedStocks++;
                meterRegistry.counter("market.candle.warmup.failures").increment();
                log.warn("홈 캔들 워밍업에 실패했습니다. stockId={}, startTime={}, endTime={}",
                        stockId, startTime, endTime, exception);
            }
        }

        meterRegistry.counter("market.candle.warmup.saved.candles").increment(savedCandles);
        log.info("홈 캔들 워밍업 완료. lastTradingDay={}, target={}, stale={}, warmed={}, savedCandles={}, failed={}",
                lastTradingDay, targetStockIds.size(), staleStockIds.size(), warmedStocks, savedCandles, failedStocks);

        return WarmUpResult.completed(
                lastTradingDay, targetStockIds.size(), staleStockIds.size(), warmedStocks, failedStocks);
    }

    /**
     * 4개 랭킹의 상위 {@code topN}을 합집합으로 모은다.
     * 같은 종목이 여러 랭킹에 겹치는 경우가 많아 실제 대상은 40종목보다 작다.
     */
    private List<Long> collectTopRankedStockIds() {
        Set<Long> stockIds = new LinkedHashSet<>();
        for (RankType rankType : HOME_RANK_TYPES) {
            stockRankingRepository.findByRankType(rankType).stream()
                    .filter(ranking -> ranking.getRank() != null && ranking.getRank() <= topN)
                    .sorted(Comparator.comparing(StockRanking::getRank))
                    .map(StockRanking::getStockId)
                    .filter(Objects::nonNull)
                    .forEach(stockIds::add);
        }
        return List.copyOf(stockIds);
    }

    /**
     * 마지막 거래일 일봉을 이미 가진 종목은 건드리지 않는다.
     * 결측 마커(isMissing)는 최신 캔들 조회에서 제외되므로 자연스럽게 재시도 대상이 된다.
     */
    private List<Long> findStaleStockIds(List<Long> targetStockIds, LocalDate lastTradingDay) {
        LocalDateTime lastTradingAt = lastTradingDay.atStartOfDay();

        Set<Long> freshStockIds = priceCandleRepository
                .findLatestByStockIdsAndTimeframe(targetStockIds, Timeframe.DAY).stream()
                .filter(candle -> !candle.getAt().isBefore(lastTradingAt))
                .map(PriceCandle::getStockId)
                .collect(Collectors.toSet());

        return targetStockIds.stream()
                .filter(stockId -> !freshStockIds.contains(stockId))
                .toList();
    }

    private int warmUpStock(Long stockId, LocalDateTime startTime, LocalDateTime endTime) {
        List<PriceCandleDto.Response> fetchedCandles =
                realMarketClient.fetchPriceCandles(stockId, startTime, endTime, Timeframe.DAY);

        if (fetchedCandles == null || fetchedCandles.isEmpty()) {
            meterRegistry.counter("market.candle.warmup.provider.empty").increment();
            log.warn("워밍업 대상 일봉을 외부에서 받지 못했습니다. stockId={}, startTime={}, endTime={}",
                    stockId, startTime, endTime);
            return 0;
        }

        Map<LocalDateTime, PriceCandle> existingByAt = priceCandleRepository
                .findExisting(stockId, startTime, endTime, Timeframe.DAY).stream()
                .collect(Collectors.toMap(PriceCandle::getAt, candle -> candle, (left, right) -> left));

        List<PriceCandle> changedCandles = new ArrayList<>();
        for (PriceCandleDto.Response fetched : fetchedCandles) {
            PriceCandle existing = existingByAt.get(fetched.getAt());

            if (existing == null) {
                changedCandles.add(PriceCandle.create(
                        stockId,
                        Timeframe.DAY,
                        fetched.getAt(),
                        fetched.getOpen(),
                        fetched.getHigh(),
                        fetched.getLow(),
                        fetched.getClose(),
                        fetched.getPrevDayChangePct(),
                        fetched.getVolume(),
                        fetched.getValue()
                ));
                continue;
            }

            if (!existing.getIsMissing() && isUnchanged(existing, fetched)) {
                continue;
            }

            existing.restore(
                    fetched.getOpen(),
                    fetched.getHigh(),
                    fetched.getLow(),
                    fetched.getClose(),
                    fetched.getPrevDayChangePct(),
                    fetched.getVolume(),
                    fetched.getValue()
            );
            changedCandles.add(existing);
        }

        if (changedCandles.isEmpty()) {
            return 0;
        }

        priceCandleRepository.saveAll(changedCandles);
        return changedCandles.size();
    }

    /** 확정된 과거 일봉은 값이 그대로인 경우가 대부분이라 불필요한 UPDATE를 막는다. */
    private boolean isUnchanged(PriceCandle existing, PriceCandleDto.Response fetched) {
        return isSameNumber(existing.getOpen(), fetched.getOpen())
                && isSameNumber(existing.getHigh(), fetched.getHigh())
                && isSameNumber(existing.getLow(), fetched.getLow())
                && isSameNumber(existing.getClose(), fetched.getClose())
                && isSameNumber(existing.getVolume(), fetched.getVolume())
                && isSameNumber(existing.getValue(), fetched.getValue());
    }

    private boolean isSameNumber(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    public record WarmUpResult(
            LocalDate tradingDate,
            String skipReason,
            int targetStockCount,
            int staleStockCount,
            int warmedStockCount,
            int failedStockCount
    ) {
        static WarmUpResult skipped(LocalDate tradingDate, String reason) {
            return new WarmUpResult(tradingDate, reason, 0, 0, 0, 0);
        }

        static WarmUpResult completed(
                LocalDate tradingDate,
                int targetStockCount,
                int staleStockCount,
                int warmedStockCount,
                int failedStockCount
        ) {
            return new WarmUpResult(
                    tradingDate, null, targetStockCount, staleStockCount, warmedStockCount, failedStockCount);
        }

        public boolean isSkipped() {
            return skipReason != null;
        }
    }
}
