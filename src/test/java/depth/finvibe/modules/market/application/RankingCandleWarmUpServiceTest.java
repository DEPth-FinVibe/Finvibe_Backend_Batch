package depth.finvibe.modules.market.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import depth.finvibe.modules.market.application.port.out.PriceCandleRepository;
import depth.finvibe.modules.market.application.port.out.RealMarketClient;
import depth.finvibe.modules.market.application.port.out.StockRankingRepository;
import depth.finvibe.modules.market.domain.PriceCandle;
import depth.finvibe.modules.market.domain.StockRanking;
import depth.finvibe.modules.market.domain.enums.RankType;
import depth.finvibe.modules.market.domain.enums.Timeframe;
import depth.finvibe.modules.market.dto.PriceCandleDto;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RankingCandleWarmUpServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2026, 8, 28);
    private static final LocalDate LAST_TRADING_DAY = LocalDate.of(2026, 8, 27);

    @Mock
    private StockRankingRepository stockRankingRepository;
    @Mock
    private PriceCandleRepository priceCandleRepository;
    @Mock
    private RealMarketClient realMarketClient;
    @Mock
    private HolidayCalendarService holidayCalendarService;

    private SimpleMeterRegistry meterRegistry;
    private RankingCandleWarmUpService service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new RankingCandleWarmUpService(
                stockRankingRepository,
                priceCandleRepository,
                realMarketClient,
                holidayCalendarService,
                meterRegistry
        );
        ReflectionTestUtils.setField(service, "topN", 10);
        ReflectionTestUtils.setField(service, "months", 3);

        when(holidayCalendarService.getLastTradingDayOnOrBefore(BASE_DATE))
                .thenReturn(Optional.of(LAST_TRADING_DAY));
    }

    @Test
    @DisplayName("거래대금·거래량·급상승·급하락 상위 10종목을 모두 워밍업 대상에 포함한다")
    void warmUp_coversAllFourRankingTopTen() {
        stubRanking(RankType.TOP_VALUE, 1000L);
        stubRanking(RankType.TOP_VOLUME, 2000L);
        stubRanking(RankType.TOP_RISING, 3000L);
        stubRanking(RankType.TOP_FALLING, 4000L);
        when(priceCandleRepository.findLatestByStockIdsAndTimeframe(anyList(), eq(Timeframe.DAY)))
                .thenReturn(List.of());
        when(realMarketClient.fetchPriceCandles(any(), any(), any(), eq(Timeframe.DAY)))
                .thenReturn(List.of());

        RankingCandleWarmUpService.WarmUpResult result = service.warmUpAt(BASE_DATE);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(priceCandleRepository).findLatestByStockIdsAndTimeframe(captor.capture(), eq(Timeframe.DAY));

        // 4개 랭킹 × 10종목, 겹치는 종목이 없으므로 40종목이 그대로 대상이 된다.
        assertThat(captor.getValue()).hasSize(40);
        for (long base : List.of(1000L, 2000L, 3000L, 4000L)) {
            for (int rank = 1; rank <= 10; rank++) {
                assertThat(captor.getValue()).contains(base + rank);
            }
        }
        assertThat(result.isSkipped()).isFalse();
        assertThat(result.targetStockCount()).isEqualTo(40);
    }

    @Test
    @DisplayName("상위 10위 밖 종목은 워밍업 대상에서 제외한다")
    void warmUp_excludesStocksBelowTopN() {
        when(stockRankingRepository.findByRankType(RankType.TOP_VALUE))
                .thenReturn(List.of(ranking(11L, RankType.TOP_VALUE, 1), ranking(99L, RankType.TOP_VALUE, 11)));
        when(priceCandleRepository.findLatestByStockIdsAndTimeframe(anyList(), eq(Timeframe.DAY)))
                .thenReturn(List.of());
        when(realMarketClient.fetchPriceCandles(any(), any(), any(), eq(Timeframe.DAY)))
                .thenReturn(List.of());

        service.warmUpAt(BASE_DATE);

        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(priceCandleRepository).findLatestByStockIdsAndTimeframe(captor.capture(), eq(Timeframe.DAY));
        assertThat(captor.getValue()).containsExactly(11L);
    }

    @Test
    @DisplayName("마지막 거래일 일봉을 이미 가진 종목은 외부를 호출하지 않는다")
    void warmUp_freshStock_skipsProvider() {
        when(stockRankingRepository.findByRankType(RankType.TOP_VALUE))
                .thenReturn(List.of(ranking(11L, RankType.TOP_VALUE, 1)));
        when(priceCandleRepository.findLatestByStockIdsAndTimeframe(anyList(), eq(Timeframe.DAY)))
                .thenReturn(List.of(dayCandle(11L, LAST_TRADING_DAY.atStartOfDay())));

        RankingCandleWarmUpService.WarmUpResult result = service.warmUpAt(BASE_DATE);

        verify(realMarketClient, never()).fetchPriceCandles(any(), any(), any(), any());
        assertThat(result.staleStockCount()).isZero();
    }

    @Test
    @DisplayName("받아온 일봉 중 DB에 없는 것만 저장한다")
    void warmUp_savesOnlyNewCandles() {
        LocalDateTime existingAt = LAST_TRADING_DAY.minusDays(1).atStartOfDay();
        LocalDateTime newAt = LAST_TRADING_DAY.atStartOfDay();

        when(stockRankingRepository.findByRankType(RankType.TOP_VALUE))
                .thenReturn(List.of(ranking(11L, RankType.TOP_VALUE, 1)));
        when(priceCandleRepository.findLatestByStockIdsAndTimeframe(anyList(), eq(Timeframe.DAY)))
                .thenReturn(List.of());
        when(priceCandleRepository.findExisting(eq(11L), any(), any(), eq(Timeframe.DAY)))
                .thenReturn(List.of(dayCandle(11L, existingAt)));
        when(realMarketClient.fetchPriceCandles(eq(11L), any(), any(), eq(Timeframe.DAY)))
                .thenReturn(List.of(candleResponse(11L, existingAt), candleResponse(11L, newAt)));

        service.warmUpAt(BASE_DATE);

        ArgumentCaptor<List<PriceCandle>> captor = ArgumentCaptor.forClass(List.class);
        verify(priceCandleRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(PriceCandle::getAt).containsExactly(newAt);
    }

    @Test
    @DisplayName("결측으로 표시된 슬롯은 실제 데이터로 복원한다")
    void warmUp_restoresMissingMarker() {
        LocalDateTime markerAt = LAST_TRADING_DAY.atStartOfDay();
        PriceCandle marker = PriceCandle.createMissing(11L, Timeframe.DAY, markerAt);

        when(stockRankingRepository.findByRankType(RankType.TOP_VALUE))
                .thenReturn(List.of(ranking(11L, RankType.TOP_VALUE, 1)));
        when(priceCandleRepository.findLatestByStockIdsAndTimeframe(anyList(), eq(Timeframe.DAY)))
                .thenReturn(List.of());
        when(priceCandleRepository.findExisting(eq(11L), any(), any(), eq(Timeframe.DAY)))
                .thenReturn(List.of(marker));
        when(realMarketClient.fetchPriceCandles(eq(11L), any(), any(), eq(Timeframe.DAY)))
                .thenReturn(List.of(candleResponse(11L, markerAt)));

        service.warmUpAt(BASE_DATE);

        assertThat(marker.getIsMissing()).isFalse();
        assertThat(marker.getClose()).isEqualByComparingTo("70500");
        verify(priceCandleRepository).saveAll(List.of(marker));
    }

    @Test
    @DisplayName("랭킹 데이터가 없으면 건너뛴다")
    void warmUp_noRanking_skips() {
        RankingCandleWarmUpService.WarmUpResult result = service.warmUpAt(BASE_DATE);

        assertThat(result.isSkipped()).isTrue();
        assertThat(result.skipReason()).isEqualTo("NO_RANKING");
        verify(realMarketClient, never()).fetchPriceCandles(any(), any(), any(), any());
    }

    @Test
    @DisplayName("거래일 달력을 못 구하면 외부 호출 없이 건너뛴다")
    void warmUp_calendarUnavailable_skips() {
        stubRanking(RankType.TOP_VALUE, 1000L);
        when(holidayCalendarService.getLastTradingDayOnOrBefore(BASE_DATE)).thenReturn(Optional.empty());

        RankingCandleWarmUpService.WarmUpResult result = service.warmUpAt(BASE_DATE);

        assertThat(result.isSkipped()).isTrue();
        assertThat(result.skipReason()).isEqualTo("CALENDAR_UNAVAILABLE");
        verify(realMarketClient, never()).fetchPriceCandles(any(), any(), any(), any());
    }

    @Test
    @DisplayName("한 종목이 실패해도 나머지 종목 워밍업은 계속한다")
    void warmUp_oneStockFails_continuesOthers() {
        when(stockRankingRepository.findByRankType(RankType.TOP_VALUE))
                .thenReturn(List.of(ranking(11L, RankType.TOP_VALUE, 1), ranking(12L, RankType.TOP_VALUE, 2)));
        when(priceCandleRepository.findLatestByStockIdsAndTimeframe(anyList(), eq(Timeframe.DAY)))
                .thenReturn(List.of());
        when(realMarketClient.fetchPriceCandles(eq(11L), any(), any(), eq(Timeframe.DAY)))
                .thenThrow(new IllegalStateException("provider down"));
        when(realMarketClient.fetchPriceCandles(eq(12L), any(), any(), eq(Timeframe.DAY)))
                .thenReturn(List.of(candleResponse(12L, LAST_TRADING_DAY.atStartOfDay())));
        when(priceCandleRepository.findExisting(eq(12L), any(), any(), eq(Timeframe.DAY)))
                .thenReturn(List.of());

        RankingCandleWarmUpService.WarmUpResult result = service.warmUpAt(BASE_DATE);

        assertThat(result.failedStockCount()).isEqualTo(1);
        assertThat(result.warmedStockCount()).isEqualTo(1);
        assertThat(meterRegistry.counter("market.candle.warmup.failures").count()).isEqualTo(1.0);
    }

    private void stubRanking(RankType rankType, long idBase) {
        List<StockRanking> rankings = new java.util.ArrayList<>();
        for (int rank = 1; rank <= 10; rank++) {
            rankings.add(ranking(idBase + rank, rankType, rank));
        }
        when(stockRankingRepository.findByRankType(rankType)).thenReturn(rankings);
    }

    private StockRanking ranking(Long stockId, RankType rankType, int rank) {
        return StockRanking.create(stockId, rankType, rank);
    }

    private PriceCandle dayCandle(Long stockId, LocalDateTime at) {
        return PriceCandle.create(
                stockId,
                Timeframe.DAY,
                at,
                new BigDecimal("70000"),
                new BigDecimal("71000"),
                new BigDecimal("69000"),
                new BigDecimal("70500"),
                new BigDecimal("1.25"),
                new BigDecimal("1000"),
                new BigDecimal("70500000")
        );
    }

    private PriceCandleDto.Response candleResponse(Long stockId, LocalDateTime at) {
        return PriceCandleDto.Response.builder()
                .stockId(stockId)
                .timeframe(Timeframe.DAY)
                .at(at)
                .open(new BigDecimal("70000"))
                .high(new BigDecimal("71000"))
                .low(new BigDecimal("69000"))
                .close(new BigDecimal("70500"))
                .prevDayChangePct(new BigDecimal("1.25"))
                .volume(new BigDecimal("1000"))
                .value(new BigDecimal("70500000"))
                .build();
    }
}
